package org.arepo;

import org.codejargon.fluentjdbc.api.query.Mapper;
import org.codejargon.fluentjdbc.api.query.Query;

import java.util.List;
import java.util.Set;

import static org.arepo.ASettings.*;
import static org.arepo.Response.*;

public class StatusModel {
    final Query query;
    Mapper<StatusFile> mapper = Model.objectMappers.forClass(StatusFile.class);
    public StatusModel(Query query) {
        this.query = query;
    }
    public StatusResult status3(Set<String> folders, Long version) {
        var result = new StatusResult();
        for(var rootfolder : folders) {
            var sql2 = """
                    SELECT
                        COALESCE(t.name, f.name) as 'name',
                        COALESCE(t.hash, f.hash) as 'hash',
                        CASE
                           WHEN f.name IS NULL THEN '%4$s'
                           WHEN t.hash != f.hash THEN '%6$s'
                           WHEN f.hash IS NULL THEN '%7$s'
                           WHEN t.name IS NULL THEN '%5$s'
                           ELSE '%8$s'
                           END 'action'
                    FROM ( SELECT * FROM %1$s WHERE rootfolder = '%3$s') AS f
                    FULL OUTER JOIN ( SELECT * FROM %2$s WHERE rootfolder = '%3$s') AS t ON
                        t.name = f.name;
                """;
            var sql3 = String.format(sql2, FILE_TABLE, TEMP_TABLE, rootfolder, FILE_ADDED, FILE_DELETED, FILE_MODIFIED, FILE_ERASED, FILE_NONE);
            List<StatusFile> res = query.select(sql3).listResult(mapper);
            result.update(res, version);
            //System.out.println("sql:"+sql3);
        }
        return result;
    }
    @Deprecated
    public StatusResult status2() {
        var result = new StatusResult();

        var sql = """
                SELECT
                    ifnull(%2$s.name, %1$s.name) as 'name',
                    CASE
                           WHEN %1$s.name IS NULL THEN '%3$s'
                           WHEN %2$s.hash != %1$s.hash THEN '%5$s'
                           WHEN %1$s.hash IS NULL THEN '%6$s'
                           WHEN %2$s.name IS NULL THEN '%4$s'
                           ELSE ''
                       END 'action'
                FROM %1$s
                FULL OUTER JOIN %2$s ON
                    %2$s.name = %1$s.name;
        """;
        var sql2 = """
                SELECT
                    COALESCE(%2$s.name, %1$s.name) as 'name',
                    CASE
                           WHEN %1$s.name IS NULL THEN '%3$s'
                           WHEN %2$s.hash != %1$s.hash THEN '%5$s'
                           WHEN %1$s.hash IS NULL THEN '%6$s'
                           WHEN %2$s.name IS NULL THEN '%4$s'
                           ELSE ''
                       END 'action'
                FROM %1$s
                FULL OUTER JOIN %2$s ON
                    %2$s.name = %1$s.name;
                """;
        /*
        var res = namedParamJdbcTemplate.query(
                String.format(sql2, Model.TABLE, Model.TEMP_TABLE, FILE_ADDED, FILE_DELETED, FILE_MODIFIED, FILE_ERASED, ERROR),
                stringRowMapper2
        );*/
        List<StatusFile> res = query.select(String.format(sql2, FILE_TABLE, TEMP_TABLE, FILE_ADDED, FILE_DELETED, FILE_MODIFIED, FILE_ERASED, ERROR)).listResult(mapper);

        /*
        for(var t : res){
            if(t.action.equals(FILE_ADDED)){
                result.getAddedFiles().add(t.name);
            } else if (t.action.equals(FILE_MODIFIED)) {
                result.getModifiedFiles().add(t.name);
            } else if (t.action.equals(FILE_DELETED)) {
                result.getDeletedFiles().add(t.name);
            }else if (t.action.equals(FILE_ERASED)) {
                result.getErasedFiles().add(t.name);
            } else if (t.action.equals(ERROR)) {
                //result.getWrongFiles().add(t.getName());
            }
        }*/
        return result;
    }
}
