package org.arepo;


import org.codejargon.fluentjdbc.api.mapper.Mappers;
import org.codejargon.fluentjdbc.api.query.Mapper;
import org.codejargon.fluentjdbc.api.query.Query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.arepo.ASettings.*;
import static org.arepo.AppGlobal.getCurrentTimeInSeconds;

public class LogsModel {
    final Query query;
    Mapper<Entities.LogFiles> mapper = Model.objectMappers.forClass(Entities.LogFiles.class);
    public static String createTABLE = """
            CREATE TABLE IF NOT EXISTS %s
                (id INTEGER PRIMARY KEY NOT NULL,
                comment TEXT NOT NULL UNIQUE,
                email TEXT NOT NULL,
                files JSON NOT NULL,
                created INTEGER,
                updated INTEGER
                );
            """;
    public static final Long changedLogFlag = 1L;
    public LogsModel(Query query) {
        this.query = query;
    }
    public void createTables(){
        query.update(createTABLE.formatted(LOG_TABLE)).run();
    }
    public List<Entities.LogFiles> getLogsAfterCreated(Long version) {
        var sql = "SELECT * FROM %s WHERE id > ? ORDER BY created ASC;".formatted(LOG_TABLE);
        return query.select(sql).params(version).listResult(mapper);
    }
    public List<Entities.LogFiles> findLastLogs(int lastCountRows){
        var sql = "SELECT DISTINCT * FROM %s ORDER BY created DESC LIMIT %d;".formatted(LOG_TABLE, lastCountRows);
        return query.select(sql).listResult(mapper);
    }
    public List<Entities.LogFiles> all(){
        return query.select(all.formatted(LOG_TABLE)).listResult(mapper);
    }
    public Set<String> insertLogFiles(List<Entities.LogFiles> list){
        var result = new HashSet<String>();
        for(var log : list){
            insertLogFile(log);
            result.addAll(log.getFilesList().stream().map(it->it.getName()).toList());
        }
        return result;
    }
    public void insertLogFile(Entities.LogFiles file){
        var sql = """
                INSERT INTO %s 
            (id, comment, email, files, created, updated) 
            values (?, ?, ?, json(?), ?, ?);
                """.formatted(LOG_TABLE);
        query.update(sql).params(file.id, file.comment, file.email, file.files, file.created, file.updated).run();
    }
    public List<Entities.LogFiles> findLogsByFilename(String filename){
        if(filename.isEmpty()) return List.of();
        //var sql = "SELECT DISTINCT l.* FROM %s AS l, json_each(json_extract(p.files, '$')) WHERE name = ?;".formatted(LOG_TABLE);
        var sql = "SELECT DISTINCT l.*, json_extract(value, '$.name') AS tid FROM %s AS l, json_each(json_extract(l.files, '$')) WHERE tid = ?;".formatted(LOG_TABLE);
        //var sql = "SELECT json_extract(value, '$.name') AS filename FROM %s AS p, json_each(json_extract(p.filenames, '$')) WHERE p.patchname = ? AND filename = ? ;".formatted(PATCH_TABLE);
        return query.select(sql).params(filename).listResult(mapper);
    }

    public long updateLog(String email, Long version, String comment) {
        var sql = "UPDATE %s SET updated = ?, comment = ? WHERE email = ? AND id = ?;".formatted(LOG_TABLE);
        return query.update(sql).params(changedLogFlag, comment, email, version).run().affectedRows();
    }

    public List<Entities.LogFiles> getLogsAfterVersion(Long version) {
        var sql = "SELECT * FROM %s WHERE updated == ? AND id >= ?;".formatted(LOG_TABLE);
        return query.select(sql).params(changedLogFlag, version).listResult(mapper);
    }

    public long update(Entities.LogFiles logfile) {
        var sql = "UPDATE %s SET comment = ?, updated = ? WHERE id = ?;".formatted(LOG_TABLE);
        return query.update(sql).params(logfile.comment, logfile.updated, logfile.id).run().affectedRows();
    }
}
