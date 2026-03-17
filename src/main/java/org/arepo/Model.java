package org.arepo;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import org.codejargon.fluentjdbc.api.mapper.ObjectMappers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.arepo.ASettings.*;
import static org.arepo.AppGlobal.*;
import static org.arepo.Response.*;

public class Model {
    Path filename;
    Path dirPath;
    DBStorage storage;

    public TextModel2 textModel;
    public PatchModel patchModel;
    public TempModel2 tempModel;
    public StatusModel statusModel;
    public LogsModel logsModel;

    static ObjectMappers objectMappers = ObjectMappers.builder().build();
    public Model(Path repo) {
        this.dirPath = repo;
        try{
            this.filename = Files.createDirectories(dirPath.toAbsolutePath().resolve(DIR_NAME)).resolve(DB_NAME);
            this.storage = new DBStorage(DB_PREFIX+this.filename.toString());
            this.textModel = new TextModel2(storage.query);
            this.patchModel = new PatchModel(storage.query);
            this.tempModel = new TempModel2(storage.query);
            this.statusModel = new StatusModel(storage.query);
            this.logsModel = new LogsModel(storage.query);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save(RepoSettings settings) throws IOException {
        Files.write(dirPath.toAbsolutePath().resolve(DIR_NAME).resolve(FILE_SETTINGS), gson.toJson(settings).getBytes());
    }
    public RepoSettings get() throws IOException {
        return gson.fromJson(Files.readString(dirPath.toAbsolutePath().resolve(DIR_NAME).resolve(FILE_SETTINGS)), RepoSettings.class);
    }

    public void create() throws Exception {
        textModel.createTables();
        patchModel.createTables();
        patchModel.addDefaultValues();
        tempModel.createTable();
        logsModel.createTables();
    }

    public Long addPatchToLogs(String patchname, String email) throws IOException {
        var textFiles = new ArrayList<Entities.TextFiles>();
        var mapUniDiff = new HashMap<String, byte[]>();

        var settings = get();
        Long version = settings.version + 1L;//getCurrentTimeInSeconds();
        var foundedPatch = patchModel.findByPatchname(patchname);
        if(foundedPatch.isEmpty()) throw new IOException("Not found patch:"+patchname);
        var patch = foundedPatch.get(0);
        var errors = new ArrayList<String>();
        var files = patch.getFilenamesList();
        for(var filename : files){
            var founded = textModel.findByName(filename.getName());
            var text = !founded.isEmpty()?founded.iterator().next():new Entities.TextFiles(0L, filename.rootfolder, filename.getName(), filename.getContent());
            if (!text.getHash().equals(filename.getPrevHash())){
                if(!text.getId().equals(0L)){
                    errors.add("File %s has conflict prev hash %s with last hash: %s \n".formatted(filename.getName(), filename.getPrevHash(), text.getHash()));
                }
            }
            /*
            if(!filename.getPrevHash().equals(DEFAULT_LAST_HASH)){
                if(!filename.getPrevHash().equals(text.getHash())){
                    errors.add("File %s has conflict prev hash %s with last hash: %s \n".formatted(filename.getName(), filename.getPrevHash(), text.getHash()));
                }
            }

             */

            if(filename.getContent() == null) {
                text.setName(ASettings.SIGN_ERASIED_FILE +filename.getName());
                text.update(filename.getContent());
            }else {
                if(!filename.getCharset().equals(BINARY_CHARSET)) {
                    // create uniDiff for text file.
                    List<String> original = text.getId() == 0L?List.of(): Arrays.asList(new String(text.getContent(), FileCharsets.findByIndex(text.getCharset())).split(DELIMITER));
                    //var target = Arrays.asList(new String(file.getContent(), FileCharsets.findByIndex(file.getCharset())).split(GlobalValues.DELIMITER));
                    var first = text.getId() == 0L?"":new String(text.getContent(), FileCharsets.findByIndex(text.getCharset()));
                    var second = new String(filename.getContent(), FileCharsets.findByIndex(filename.getCharset()));
                    var diffExp = DiffUtils.diff(first, second, null);
                    List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff("original", "new", original, diffExp, 0);
                    mapUniDiff.put(text.getName(), String.join(DELIMITER, unifiedDiff).getBytes());
                }
                text.update(filename.getContent());
            }
            text.setCharset(filename.getCharset());
            textFiles.add(text);
        }
        if(!errors.isEmpty()) throw new IOException(errors.toString());
        for(var tfile : textFiles){
            if(tfile.getId().equals(0L)){
                tfile.setVersion(version);
                textModel.add(tfile);
            }else {
                tfile.setVersion(version);
                textModel.update(tfile);
            }
            tfile.setContent(mapUniDiff.getOrDefault(tfile.getName(), tfile.getContent()));
        }
        logsModel.insertLogFile(new Entities.LogFiles(version, patch.comment, email, gson.toJson(textFiles), System.currentTimeMillis()));
        patchModel.delete(patchname);
        settings.version = version;
        save(settings);
        return version;
    }
}
