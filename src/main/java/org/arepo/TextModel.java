package org.arepo;

import org.codejargon.fluentjdbc.api.mapper.Mappers;
import org.codejargon.fluentjdbc.api.query.Mapper;
import org.codejargon.fluentjdbc.api.query.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.arepo.ASettings.*;

public class TextModel {
    // mappers for POJO classes
    Mapper<Entities.TextFiles> mapper = Model.objectMappers.forClass(Entities.TextFiles.class);
    //Mapper<Entities.WorkReport> mapperReport = ProjectStorage.objectMappers.forClass(Entities.WorkReport.class);
    public static String createTABLE = """
            CREATE TABLE IF NOT EXISTS %s
                        (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL UNIQUE,
                        content BLOB,
                        hash INTEGER,
                        parent INTEGER,
                        version INTEGER NOT NULL DEFAULT 0,
                        charset INTEGER
                        );
            """;
    final Query query;
    //Mapper<Entities.TextFiles> mapper = new RecordMapper<>(Entities.TextFiles.class);
    public TextModel(Query query) {
        this.query = query;
    }
    public void createTables(){
        query.update(createTABLE.formatted(FILE_TABLE)).run();
    }
    public void add(Entities.TextFiles notes){
        //var sql = "INSERT INTO %s (title, folderId, created, modified) VALUES (?,?,?,?);";
        var sql = """
                INSERT INTO %s 
            (name, content, hash, parent, version, charset) 
            values (?, ?, ?, ?, ?, ?);
                """;
        var id = query.update(sql.formatted(FILE_TABLE)).params(notes.getName(), notes.getContent(), notes.getHash(), notes.getParent(), notes.getVersion(), notes.getCharset()).runFetchGenKeys(Mappers.singleLong()).generatedKeys().get(0);
        notes.setId(id);
    }
    public long update(Entities.TextFiles notes){
        var sql = """ 
                UPDATE %s SET version = ?, hash = ?, content = ?, name = ? WHERE id = ?;
                """;
        return query.update(sql.formatted(FILE_TABLE)).params(notes.getVersion(), notes.getHash(), notes.getContent(), notes.getName(), notes.getId()).run().affectedRows();
    }

    public long deleteById(Integer id){
        return query.update(deleteById.formatted(FILE_TABLE)).params(id).run().affectedRows();
    }
    public List<Entities.TextFiles> all(){
        return query.select(all.formatted(FILE_TABLE)).listResult(mapper);
    }
    public List<Entities.TextFiles> findByIds(List<Long> ids) {
        var sql = "SELECT * FROM %s WHERE id IN (?);".formatted(FILE_TABLE);
        return query.select(sql).params(ids).listResult(mapper);
    }
    public List<Entities.TextFiles> findByName(String name) {
        var sql = "SELECT DISTINCT * FROM %s WHERE name = ? AND content IS NOT NULL;".formatted(FILE_TABLE);
        return query.select(sql.formatted(FILE_TABLE)).params(name).listResult(mapper);
    }

    public List<Entities.TextFiles> getFilesAfterVersion(Long version){
        var sql = "SELECT DISTINCT * from %s WHERE version > ?;".formatted(FILE_TABLE);
        return query.select(sql).params(version).listResult(mapper);
    }
    public long deleteByIds(List<Long> ids) {
        var sql = "DELETE FROM %s WHERE id IN (:ids);".formatted(FILE_TABLE);
        return query.update(sql).namedParam("ids", ids).run().affectedRows();
    }
    public int insertFiles(List<Entities.TextFiles> textFiles) {
        var sql = """
                    INSERT INTO %s 
            (id, name, content, hash, parent, version, charset) 
            values (?,?,?,?,?,?,?);
                    """.formatted(FILE_TABLE);
        for(var notes : textFiles){
            query.update(sql).params(notes.getId(), notes.getName(), notes.getContent(), notes.getHash(), notes.getParent(), notes.getVersion(), notes.getCharset()).run();
        }
        return 0;
    }
    public Map<Long, Entities.TextFiles> findAsMapByIds(List<Long> ids){
        var sql = "SELECT * FROM %s WHERE id IN (:ids);".formatted(FILE_TABLE);
        var result = new HashMap<Long, Entities.TextFiles>(ids.size());
        var founded = query.select(sql).namedParam("ids", ids).listResult(mapper);
        founded.forEach(it -> result.put(it.getId(), it));
        return result;
    }

}
