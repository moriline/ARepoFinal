package org.arepo;

import org.codejargon.fluentjdbc.api.mapper.Mappers;
import org.codejargon.fluentjdbc.api.query.Mapper;
import org.codejargon.fluentjdbc.api.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.arepo.ASettings.*;

public class PatchModel {
    final Query query;
    Mapper<Entities.APatch> mapper = Model.objectMappers.forClass(Entities.APatch.class);
    Mapper<Entities.APatchHistory> mapperHistory = Model.objectMappers.forClass(Entities.APatchHistory.class);
    static String HISTORY = "patch_history";
    public static String createTABLE = """
            CREATE TABLE IF NOT EXISTS %s
                        (
                        patchname TEXT NOT NULL UNIQUE,
                        filenames JSON,
                        version INTEGER NOT NULL DEFAULT 0,
                        comment TEXT NOT NULL DEFAULT ''
                        );
            """;
    public static String createPATCH_TABLE_HISTORY = """        
        CREATE TABLE IF NOT EXISTS %s 
            (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,            
            patchname TEXT NOT NULL,
            filenames JSON,
            comment TEXT,
            updated INTEGER DEFAULT (unixepoch())
            );            
        """;
    public static String TRIGGER = """
            CREATE TRIGGER archive_patch_update
            AFTER UPDATE ON %s
            FOR EACH ROW
            BEGIN
                INSERT INTO %s (
                    patchname,
                    filenames,
                    comment
                )
                VALUES (
                    OLD.patchname,
                    OLD.filenames,
                    OLD.comment
                );
            END;   
            """;
    public PatchModel(Query query) {
        this.query = query;
    }
    public void createTables(){
        query.update(createTABLE.formatted(PATCH_TABLE)).run();
        query.update("PRAGMA foreign_keys = ON;").run();
        query.update(createPATCH_TABLE_HISTORY.formatted(HISTORY)).run();
        query.update(TRIGGER.formatted(PATCH_TABLE, HISTORY)).run();
    }
    public void addDefaultValues() throws Exception {
        for(var pa : defindedPatches){
            add(new Entities.APatch(pa, List.of()));
        }
    }
    public List<Entities.APatch> findByPatchname(String patchname){
        var sql = "SELECT * FROM %s WHERE patchname = ?";
        return query.select(sql.formatted(PATCH_TABLE)).params(patchname).listResult(mapper);
    }
    public void add(Entities.APatch patch) throws Exception {
        var size = patch.getFilenamesList().size();
        if(size != patch.getFilenamesList().size()) throw new Exception("Arrays of files is not equals");
        if(size > MAX_FILES_IN_PATCH) throw new Exception("Array of files is more then:"+MAX_FILES_IN_PATCH);
        var sql = "INSERT INTO %s (patchname, filenames, version, comment) VALUES (?,json(?), ?, ?);";
        //query.update(sql.formatted(PATCH_TABLE)).params(patch.patchname, patch.filenames, patch.version, patch.comment).run();
        //batch query
        List<List<?>> filesList = new ArrayList<List<?>>(size);
        filesList.add(List.of(patch.patchname, patch.filenames, patch.version, patch.comment));
        query.transaction().inNoResult(()->{
            query.batch(sql.formatted(PATCH_TABLE)).params(filesList.iterator()).run();
        });
    }
    public long updateFilesInPatch(Entities.APatch patch) throws Exception {
        var size = patch.getFilenamesList().size();
        if(size != patch.getFilenamesList().size()) throw new Exception("Arrays of files is not equals");
        if(size > MAX_FILES_IN_PATCH) throw new Exception("Array of files is more then:"+MAX_FILES_IN_PATCH);
        var sql = "UPDATE %s SET filenames = json(?), version = ? WHERE patchname = ? ;";
        return query.update(sql.formatted(PATCH_TABLE)).params(patch.filenames, patch.version, patch.patchname).run().affectedRows();
    }
    //TODO to find path by filename use this pattern:
    // SELECT t.id, json_extract(value, '$.id') AS filename from tasks4 AS t, json_each(json_extract(t.worktimes, '$')) WHERE filename = 'alice';
    public List<String> findInPatchByFilename(String patchname, String filename){
        var sql = "SELECT json_extract(value, '$.name') AS filename FROM %s AS p, json_each(json_extract(p.filenames, '$')) WHERE p.patchname = ? AND filename = ? ;".formatted(PATCH_TABLE);
        return query.select(sql).params(patchname, filename).listResult(Mappers.singleString());
    }

    public long setComment(String patchname, String comment) {
        var sql = "UPDATE %s SET comment = ? WHERE patchname = ? ;";
        return query.update(sql.formatted(PATCH_TABLE)).params(comment, patchname).run().affectedRows();
    }

    public List<Entities.APatch> list() {
        var sql = "SELECT * FROM %s;";
        return query.select(sql.formatted(PATCH_TABLE)).listResult(mapper);
    }

    public long clearData(String patchname) {
        var emptyArray = List.of();
        var sql = "UPDATE %s SET comment = '', filenames = json(?) WHERE patchname = ? ;";
        return query.update(sql.formatted(PATCH_TABLE)).params(emptyArray, patchname).run().affectedRows();
    }
    public long delete(String patchname){
        var sql = "DELETE FROM %s WHERE patchname = ? ;";
        return query.update(sql.formatted(PATCH_TABLE)).params(patchname).run().affectedRows();
    }
    public long clearPatchHistory(String patchname){
        var sql = "DELETE FROM %s WHERE patchname = ? ;";
        return query.update(sql.formatted(HISTORY)).params(patchname).run().affectedRows();
    }
    public List<Entities.APatchHistory> findHistoryByPatchname(String patchname){
        var sql = "SELECT * FROM %s WHERE patchname = ? ORDER BY updated DESC";
        return query.select(sql.formatted(HISTORY)).params(patchname).listResult(mapperHistory);
    }
}
