package org.arepo;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.arepo.ASettings.*;
import static org.arepo.AppGlobal.*;
import static org.arepo.Response.*;

public class AFiles {
    public final Path dirPath;
    public final Model model;
    final RemotePatchService remotePatchService;
    public AFiles(Path dirPath) throws Exception {
        this.dirPath = dirPath.toAbsolutePath();
        if(Files.notExists(this.dirPath)) throw new Exception(DNE+this.dirPath);
        model = new Model(this.dirPath);
        remotePatchService = new RemotePatchService();
    }
    public AFiles(String path)throws Exception{
        //if(path.isEmpty()) throw new Exception("empty");
        this(Path.of(path).toAbsolutePath());
    }
    public StatusResult status3() throws IOException{
        var settings = model.get();
        var otherFiles = this.getFSFiles();
        if(settings.useFS){
            model.tempModel.dropTable();
            model.tempModel.createTable();
            model.tempModel.batchInsertTempFiles(otherFiles);
        }
        HashSet<String> rootFolders = new HashSet<String>(model.textModel.getRootFolders());
        otherFiles.keySet().forEach(rootFolders::add);
        var result = model.statusModel.status3(rootFolders, settings.version);
        return result;
    }

    /**
     * Get Map of files with parent folder as key, and value - Map with filename and content.
     * @return Map
     * @throws IOException
     */
    public Map<String, Map<String, byte[]>> getFSFiles() throws IOException {
        var settings = model.get();
        var ignoreDirs = new HashSet<String>(settings.ignoreDirs);
        ignoreDirs.add(DIR_NAME);
        var otherFiles = FSUtils.getAllFiles7(dirPath, settings.ignoreFiles, ignoreDirs);
        return otherFiles;
    }
    public StatusResult status() throws IOException {
        return this.status3();
        /*
        var settings = model.get();
        if(settings.useFS){
            model.tempModel.dropTable();
            model.tempModel.createTable();
            model.tempModel.add(dirPath, settings.ignoreDirs, settings.ignoreFiles);
        }
        var result = model.statusModel.status2();
        result.version = settings.version;
        return result;

         */
    }
    public String setComment(String patchname, String comment) throws Exception {
        if(comment.isEmpty()) throw new Exception("comment is empty");
        var result = model.patchModel.setComment(patchname, comment);
        return String.valueOf(result);
    }

    /**
     * Send patch with patchname like 'a' to Root repo.
     *
     * @param patchname
     * @param local
     * @return
     * @throws Exception
     */
    public String sendPatch(String patchname, boolean local) throws Exception{
        if(!ASettings.defindedPatches.contains(patchname)) throw new Exception("Invalid patch name:"+patchname);
        var settings = model.get();
        if(settings.remote.isEmpty()) throw new Exception(WAFR+dirPath);
        var patchFiles = model.patchModel.findByPatchname(patchname);
        if(patchFiles.isEmpty()) throw new Exception(PIE+dirPath);
        var patch = patchFiles.get(0);
        if(patch.comment.isEmpty()) throw new Exception("Comment is empty for patch:"+patchname);
        patch.version = settings.version;
        var remotePatchname = settings.email+"_"+System.currentTimeMillis(); //getTimeInSec();
        patch.patchname = remotePatchname;
        if(local){
            // save patch to local file
            Files.write(dirPath.toAbsolutePath().resolve(DIR_NAME).resolve(remotePatchname), gson.toJson(patch).getBytes());
        }else {
            remotePatchService.sendPatch(settings.remote, patch, settings.email);
        }
        return remotePatchname;
    }

    /**
     * Get patch from Root repo
     *
     * @param patchname
     * @param local
     * @return
     * @throws Exception
     */
    public List<Entities.APatch> getPatch(String patchname, boolean local) throws Exception {
        var settings = model.get();
        if(settings.remote.isEmpty()) throw new Exception(WAFR+dirPath);
        var loaded = new ArrayList<Entities.APatch>();
        if(local){
            //List<Entities.APatch>
            loaded.add(gson.fromJson(Files.readString(dirPath.resolve(DIR_NAME).resolve(patchname)), Entities.APatch.class));
        }else {
            loaded.addAll(remotePatchService.getPatch(settings.remote, patchname));
        }
        if(!loaded.isEmpty()){
            for(var pa : loaded){
                model.patchModel.add(pa);
            }
        }
        return loaded;
    }
    public long clearPatch(String patchname) throws Exception {
        if(!ASettings.defindedPatches.contains(patchname)) throw new Exception("Invalid patch name:"+patchname);
        var result = model.patchModel.clearData(patchname);
        model.patchModel.clearPatchHistory(patchname);
        return result;
    }
    public long deletePatch(String patchname) throws Exception {
        if(ASettings.defindedPatches.contains(patchname)) throw new Exception("Invalid patch name:"+patchname);
        return model.patchModel.delete(patchname);
    }
    public List<Entities.APatchHistory> getHistory(String patchname) throws Exception {
        if (!ASettings.defindedPatches.contains(patchname)) throw new Exception("Invalid patch name:" + patchname);
        return model.patchModel.findHistoryByPatchname(patchname);
    }
    public long deleteRemotePatch(String patchname) throws Exception{
        if(ASettings.defindedPatches.contains(patchname)) throw new Exception("Invalid patch name:"+patchname);
        var settings = model.get();
        if(settings.remote.isEmpty()) throw new Exception(WAFR+dirPath);
        return remotePatchService.deletePatch(settings.remote, patchname, settings.email);
    }
    public List<Entities.ATempFiles> applyPatch2FS(String patchname) throws Exception {
        //var files = model.patch.getFilesFromPatch(patchname);
        var founded = model.patchModel.findByPatchname(patchname);
        if(founded.isEmpty()) throw new Exception("Patch not founded:"+patchname);
        var patch = founded.get(0);
        var files = patch.getFilenamesList();
        for(var file : files){
            var local = dirPath.resolve(file.getName()).toAbsolutePath();
            if(file.getContent() != null && file.getHash() != null){
                local.getParent().toFile().mkdirs();
                Files.write(local, file.getContent());
            }else {
                if(Files.exists(local)){
                    Files.delete(local);
                }
            }
        }
        return files;
    }
    public List<Entities.APatch> patches() throws Exception {
        return model.patchModel.list();
    }
    public RepoSettings setup(String root, String email, Integer eol, boolean isShortLogs) throws Exception {
        RepoSettings result = null;
        if(ASettings.ROOT_PREFIX.equals(root)){
            //this is root repo like: setup root -> FROM ''
            model.create();
            result = new RepoSettings(dirPath, email, eol);
            result.useFS = false; //это репозиторий, не имеющий рабочей директории.
            model.save(result);
        }else{
            //this is clone repo like: setup clone -> FROM root
            var rootPath = Path.of(root).toAbsolutePath();
            if(Files.notExists(rootPath)) throw new Exception(DNE+rootPath);
            var rootModel = new Model(rootPath);
            var rootSettings = rootModel.get();

            model.create();
            result = new RepoSettings(dirPath, email, rootSettings.eol);
            result.remote = rootPath.toString();
            result.version = rootSettings.version;
            model.save(result);

            var logs = isShortLogs?rootModel.logsModel.findLastLogs(DEFAULT_SHORT_LOGS_SIZE):rootModel.logsModel.all();
            var files = rootModel.textModel.all();
            if(!logs.isEmpty()) model.logsModel.insertLogFiles(logs);
            if(!files.isEmpty()) {
                model.textModel.insertFiles(files);
                fillFileSystemByFiles(dirPath, result, files);
            }
        }
        return result;
    }
    /**
     * Update repo settings for ignore dirs or files.
     * @param ignoreDirs
     * @param ignoreFiles
     * @return
     * @throws Exception
     */
    public RepoSettings updateSettings(Set<String> ignoreDirs, Set<String> ignoreFiles) throws Exception {
        //if(ignoreDirs.isEmpty()) throw new Exception("Dirs is empty!");
        //if(ignoreFiles.isEmpty()) throw new Exception("Files is empty!");
        var settings = model.get();
        settings.ignoreDirs = ignoreDirs;
        settings.ignoreFiles = ignoreFiles;
        model.save(settings);
        return settings;
    }
    public String accept(String patchname) throws Exception {
        if(ASettings.defindedPatches.contains(patchname)) throw new Exception("Invalid patch name:"+patchname);
        var settings = model.get();
        if(settings.remote.isEmpty()) throw new Exception(WAFR+dirPath);
        remotePatchService.accept(settings.remote, patchname, settings.email, settings.version);
        return "";
    }

    public String upload(String localPatchName) throws Exception {
        String remotePatchname = sendPatch(localPatchName, false);
        accept(remotePatchname);
        return remotePatchname;
    }
    public DownloadResult download() throws Exception {
        var settings = model.get();
        var version = settings.version;
        if(settings.remote.isEmpty()) throw new Exception(WAFR+dirPath);
        var result = new DownloadResult();
        var remoteResult = remotePatchService.getDataAfterVersion(settings.remote, version);
        System.out.println("local version:"+version);
        if(!remoteResult.logs.isEmpty()){

            var logList = model.logsModel.insertLogFiles(remoteResult.logs);
            result.logs.addAll(logList);
            var ids = new ArrayList<Long>();
            var names = new ArrayList<String>();
            for(var item : remoteResult.files){
                ids.add(item.getId());
                names.add(item.getName());
            }
            model.textModel.deleteByIds(ids);
            model.textModel.insertFiles(remoteResult.files);
            result.files.addAll(names);
            //save new version
            settings.version = result.version = remoteResult.version;
            model.save(settings);
        }
        return result;
    }

    /**
     * Replace all files in the local patch.
     * @param patchname name of local patch like 'a'
     * @param filenames Set of file names like: 'src/com/package/Main.java'
     * @return result of count updated files.
     * @throws Exception
     */
    public long save(String patchname, Set<String> filenames) throws Exception {
        if(!ASettings.defindedPatches.contains(patchname)) throw new Exception("Invalid patch name:"+patchname);
        //var time = getCurrentTimeInSeconds();
        var tempList = new ArrayList<Entities.ATempFiles>();
        for (var filename : filenames) {
            //filename = filename.replace("\\", ASettings.SEP);
            if (model.get().ignoreFiles.contains(filename)) continue;
            var textFilesFound = model.textModel.findByName(filename);
            Long prevHash = textFilesFound.isEmpty()? DEFAULT_LAST_HASH:textFilesFound.get(0).getHash();
            Entities.ATempFiles temp = new Entities.ATempFiles(dirPath, filename, prevHash);
            if(temp.getContent() == null){
                if(textFilesFound.isEmpty()){
                    throw new Exception(FNE+filename);
                }
            }
            tempList.add(temp);
        }
        return model.patchModel.updateFilesInPatch(new Entities.APatch(patchname, tempList));
    }
    public long removeFromPatch(String patchname, Set<String> filenames) throws Exception {
        if(!ASettings.defindedPatches.contains(patchname)) throw new Exception("Invalid patch name:"+patchname);
        var founded = model.patchModel.findByPatchname(patchname);
        if(founded.isEmpty()) throw new Exception("Patch not founded:"+patchname);
        var tempList = new ArrayList<Entities.ATempFiles>();
        var patch = founded.get(0);
        var files = patch.getFilenamesList();
        for(var file : files){
            boolean found = false;
            for (var filename : filenames) {
                //filename = filename.replace("\\", ASettings.SEP);
                if(file.getName().equals(filename)) {
                    found = true;
                    break;
                }
            }
            if(!found) tempList.add(file);
        }
        patch.filenames = gson.toJson(tempList);
        return model.patchModel.updateFilesInPatch(patch);
    }
    public Entities.APatch showPatch(String patchname) throws Exception {
        var founded = model.patchModel.findByPatchname(patchname);
        if(founded.isEmpty()) throw new Exception("Patch not founded:"+patchname);
        return founded.get(0);
    }
    public void clearFS() throws Exception {
        var settings = model.get();
        FSUtils.clear(dirPath, settings.ignoreFiles, settings.ignoreDirs);
    }
    public List<Entities.LogFiles> getLogs(int count) throws Exception {
        return model.logsModel.findLastLogs(count);
    }

    /**
     * Returns very important entities(number 1) of our application - TextFiles.
     * Every directory contains files on file system and TextFiles - snapshot of all files by last changes in root repository.
     * @return
     * @throws Exception
     */
    public List<Entities.TextFiles> getFiles() throws Exception {
        return model.textModel.all();
    }

    /**
     * Fill file system by our Files. If our Files is not empty - all other files will be deleted.
     * @throws Exception
     */
    public void clearAndHead() throws Exception {
        var settings = model.get();
        //settings.ignoreFiles.add(ASettings.DIR_NAME);
        var files = model.textModel.all();
        fillFileSystemByFiles(dirPath, settings, files);
    }
    private static void fillFileSystemByFiles(Path dirPath, RepoSettings settings, List<Entities.TextFiles> files) throws IOException {
        if(!files.isEmpty()){
            FSUtils.clear(dirPath, settings.ignoreFiles, settings.ignoreDirs);
        }
        for(var file : files){
            if(file.getName().startsWith(ASettings.SIGN_ERASIED_FILE)) continue;
            var local = dirPath.resolve(file.getName()).toAbsolutePath();
            if(file.getContent() != null){
                local.getParent().toFile().mkdirs();
                Files.write(local, file.getContent());
            }else {
                if(Files.exists(local)){
                    Files.delete(local);
                }
            }
        }
    }
    public List<Entities.LogFiles> applyFSByLogsByCountLimit(int countLogsLimit) throws Exception {
        var logtype = new TypeToken<List<Entities.TextFiles>>() {}.getType();
        var logs = model.logsModel.findLastLogs(countLogsLimit);
        var textFilesPatchesMap = new HashMap<Entities.TextFiles, List<String>>();
        for(var log : logs){
            List<Entities.TextFiles> files = gson.fromJson(log.files, logtype);
            var fileIds = files.stream().map(it->it.getId()).collect(Collectors.toList());
            var foundedMap = model.textModel.findAsMapByIds(fileIds);
            foundedMap.values().stream().forEach(it -> textFilesPatchesMap.putIfAbsent(it, new ArrayList<String>()));

            for(var file : files){
                var contentUniPatch = file.getContent();
                if(!file.getCharset().equals(BINARY_CHARSET)) {
                    //text file
                    var textFile = foundedMap.get(file.getId());
                    textFilesPatchesMap.get(textFile).add(new String(contentUniPatch));
                }else {
                    if(contentUniPatch != null){
                        Files.write(dirPath.resolve(file.getName()), contentUniPatch);
                    }else {
                        Files.delete(dirPath.resolve(file.getName()));
                    }
                }
            }
        }
        //text files
        for(var entry : textFilesPatchesMap.entrySet()){
            var textFile = entry.getKey();
            var orig = new String(textFile.getContent());
            var finalContent = applyPatches(Arrays.asList(orig.split("\n")), entry.getValue());
            var content = String.join("\n", finalContent).trim();
            //System.out.println("te:"+textFile.toString());
            //System.out.println("te 2:"+content);
            if(!content.isEmpty()){
                Files.write(dirPath.resolve(textFile.getName()), content.getBytes());
            }else {
                Files.delete(dirPath.resolve(textFile.getName()));
            }
        }
        return logs;
    }
    private static List<String> applyPatches(List<String> orig, List<String> patches){
        List<String> result = new ArrayList<String>(orig);
        for(var patchValue : patches){
            List<String> unifiedDiff = Arrays.asList(patchValue.split("\n"));
            var patch = UnifiedDiffUtils.parseUnifiedDiff(unifiedDiff);
            result = DiffUtils.unpatch(result, patch);
        }
        return result;
    }
    /**
     * Returns map of modified or deleted files in the patch to files in File system.
     * If file modified and in the conflict - would be created new file with .temp extension by patch file content from patchname.
     * Use any merge tool for resolve this conflict between bin/file.txt and bin/file.txt.temp
     * @param patchname
     * @return Map<String, String> - map of modified or deleted files.
     * @throws Exception
     */
    public Map<String, String> diff(String patchname) throws Exception {
        var result = new HashMap<String, String>();
        var founded = model.patchModel.findByPatchname(patchname);
        if(founded.isEmpty()) throw new Exception("Patch not founded:"+patchname);
        var files = founded.get(0).getFilenamesList();
        for(var file : files){
            var local = dirPath.resolve(file.getName()).toAbsolutePath();
            if(Files.exists(local)){
                var hash = getCRC32Checksum(Files.readAllBytes(local));
                //System.out.println("hash:"+hash);
                if(!file.getHash().equals(hash)){
                    Files.write(dirPath.resolve(file.getName()+ASettings.FILE_CONFLICT).toAbsolutePath(), file.getContent());
                    result.put(file.getName(), ASettings.FILE_MODIFIED);
                }
            }else {
                result.put(file.getName(), ASettings.FILE_DELETED);
            }
        }
        return result;
    }
    public List<Entities.LogFiles> logsByFilename(Set<String> filenames){
        return model.logsModel.findLogsByFilename(filenames.stream().findFirst().orElse(""));
    }
    public long updateLog(Long version, String comment) throws Exception {
        var settings = model.get();
        if(settings.remote.isEmpty()) throw new Exception(WAFR+dirPath);
        return remotePatchService.updateLog(settings.remote, settings.email, version, comment);
    }
    public List<Entities.LogFiles> getLogsAfterVersion(Long version) throws Exception {
        var settings = model.get();
        if(settings.remote.isEmpty()) throw new Exception(WAFR+dirPath);
        var logs = remotePatchService.getLogsAfterVersion(settings.remote, version);
        for(var logfile : logs){
            model.logsModel.update(logfile);
        }
        return logs;
    }

    public RepoSettings saveRepoSettings(RepoSettings settings) throws IOException {
        model.save(settings);
        return settings;
    }

    public int removeFiles(Set<String> filenames) throws IOException {
        int count = 0;
        for(var filename : filenames){
            var local = dirPath.resolve(filename).toAbsolutePath();
            if(Files.exists(local)){
                Files.delete(local);
                count++;
            }
        }

        return count;
    }

    /**
     * Creating test files if not exists.
     * @return count of created files.
     * @throws IOException
     */
    public int createFiles() throws IOException {
        int count = 0;
        var testTextFile = dirPath.resolve(TEST_TEXT_FILENAME).toAbsolutePath();
        var testBinaryFile = dirPath.resolve(TEST_BINARY_FILENAME).toAbsolutePath();
        if(!Files.exists(testTextFile)){
            Files.write(dirPath.resolve(testTextFile), TEST_TEXT_CONTENT.getBytes());count++;
        }
        if(!Files.exists(testBinaryFile)){
            Files.write(dirPath.resolve(testBinaryFile), new byte[] { -3, -4, -5, -100});count++;
        }
        return count;
    }
}
