package org.arepo;

import org.codejargon.fluentjdbc.api.query.Mapper;
import org.codejargon.fluentjdbc.api.query.Query;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.arepo.ASettings.*;

public class TempModel {
    final Query query;
    Mapper<Entities.ATempFiles> mapper = Model.objectMappers.forClass(Entities.ATempFiles.class);
    public static String createTEMP_TABLE = """
            CREATE TABLE IF NOT EXISTS %s 
            (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL UNIQUE,
            content BLOB,
            hash INTEGER
            );
            """;
    public TempModel(Query query) {
        this.query = query;
    }

    public void dropTable() {
        query.update("DROP TABLE IF EXISTS %s;".formatted(TEMP_TABLE)).run();
    }

    public void createTable() {
        query.update(createTEMP_TABLE.formatted(TEMP_TABLE)).run();
    }
    public void add(Path rootRepo, Set<String> ignoreDirs, Set<String> skipFiles) throws IOException {
        var allFiles = FSUtils.getAllFiles6(rootRepo, skipFiles, ignoreDirs);
        //insertTempFiles(allFiles);
        batchInsertTempFiles(allFiles);
    }
    public void insertTempFiles(Map<String, byte[]> files){
        var sql = """
                    INSERT INTO %s 
            (name, content, hash) 
            values (:name, :content, :hash);
                    """.formatted(TEMP_TABLE);
        //namedParamJdbcTemplate.update(sql, params, holder);
        //var list = new ArrayList<Entities.ATempFiles>(files.size());
        for(var entity : files.entrySet()){
            var temp = new Entities.ATempFiles(entity.getKey(), entity.getValue());
            //list.add(temp);
            query.update(sql).params(temp.getName(), temp.getContent(), temp.getHash()).run();
        }
    }
    public void batchInsertTempFiles(Map<String, byte[]> files){
        var sql = """
                    INSERT INTO %s 
            (name, content, hash) 
            values (?, ?, ?);
                    """.formatted(TEMP_TABLE);
        int size = files.size();
        List<List<?>> filesList = new ArrayList<List<?>>(size);
        for(var entity : files.entrySet()){
            var temp = new Entities.ATempFiles(entity.getKey(), entity.getValue());
            filesList.add(List.of(temp.getName(), temp.getContent(), temp.getHash()));
        }
        query.transaction().inNoResult(()->{
            var bq = query.batch(sql).params(filesList.iterator());
            if(size > 0) bq.batchSize(size);
            var result = bq.run();
            //System.out.println("batch res:"+result.size());
        });

    }
}
