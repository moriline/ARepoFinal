package org.arepo;

import org.codejargon.fluentjdbc.api.query.Mapper;
import org.codejargon.fluentjdbc.api.query.Query;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.arepo.ASettings.TEMP_TABLE;

public class TempModel2 {
    final Query query;
    Mapper<Entities.ATempFiles> mapper = Model.objectMappers.forClass(Entities.ATempFiles.class);
    public static String createTEMP_TABLE = """
            CREATE TABLE IF NOT EXISTS %s 
            (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            rootfolder TEXT NOT NULL,
            name TEXT NOT NULL UNIQUE,
            content BLOB,
            hash INTEGER
            );
            """;

    public TempModel2(Query query) {
        this.query = query;
    }

    public void dropTable() {
        query.update("DROP TABLE IF EXISTS %s;".formatted(TEMP_TABLE)).run();
    }

    public void createTable() {
        query.update(createTEMP_TABLE.formatted(TEMP_TABLE)).run();
    }
    public void add(Path rootRepo, Set<String> ignoreDirs, Set<String> skipFiles) throws IOException {
        var allFiles = FSUtils.getAllFiles7(rootRepo, skipFiles, ignoreDirs);
        //insertTempFiles(allFiles);
        batchInsertTempFiles(allFiles);
    }

    public void batchInsertTempFiles(Map<String, Map<String, byte[]>> files){
        var sql = """
                    INSERT INTO %s 
            (rootfolder, name, content, hash) 
            values (?, ?, ?, ?);
                    """.formatted(TEMP_TABLE);
        for(var fold : files.entrySet()){
            var rootfolder = fold.getKey();
            var files2 = fold.getValue();
            int size = files2.size();
            List<List<?>> filesList = new ArrayList<List<?>>(size);
            for(var entity : files2.entrySet()){
                var temp = new Entities.ATempFiles(entity.getKey(), entity.getValue());
                temp.rootfolder = rootfolder;
                filesList.add(List.of(temp.rootfolder, temp.getName(), temp.getContent(), temp.getHash()));
            }
            query.transaction().inNoResult(()->{
                var bq = query.batch(sql).params(filesList.iterator());
                if(size > 0) bq.batchSize(size);
                var result = bq.run();
            });
        }
    }
}
