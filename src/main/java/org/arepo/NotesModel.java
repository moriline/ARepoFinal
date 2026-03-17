package org.arepo;

import org.codejargon.fluentjdbc.api.mapper.Mappers;
import org.codejargon.fluentjdbc.api.query.Mapper;
import org.codejargon.fluentjdbc.api.query.Query;

import java.time.Month;
import java.util.List;

public class NotesModel {
    final Query query;
    //Mapper<Entities.Notes> mapper = new RecordMapper<>(Entities.Notes.class);
    public NotesModel(Query query) {
        this.query = query;
    }
    /*
    public Long add(Entities.Notes notes){
        var sql = "INSERT INTO %s (title, folderId, created, modified) VALUES (?,?,?,?);";
        return query.update(sql.formatted(AStorage.TABLE_NOTES)).params(notes.title(), notes.folderId(), notes.created(), notes.modified()).runFetchGenKeys(Mappers.singleLong()).generatedKeys().get(0);
    }
    public long update(Entities.Notes notes){
        var sql = "UPDATE %s SET title = ?, folderId = ?, modified = ? WHERE id = ?";
        return query.update(sql.formatted(AStorage.TABLE_NOTES)).params(notes.title(), notes.folderId(), notes.modified(), notes.id()).run().affectedRows();
    }
    public long deleteById(Integer id){
        return query.update(AStorage.deleteById.formatted(AStorage.TABLE_NOTES)).params(id).run().affectedRows();
    }
    public List<Entities.Notes> all(){
        return query.select(AStorage.all.formatted(AStorage.TABLE_NOTES)).listResult(mapper);
    }
    public List<Entities.Notes> findByYear(Integer year){
        return query.select(AStorage.all.formatted(AStorage.TABLE_NOTES)).listResult(mapper);
    }
    public List<Entities.Notes> findByYearAndMonth(Integer year, Integer month){
        var sql = "SELECT * FROM %s WHERE created BETWEEN ? AND ? ;";
        Long start = AppGlobal.buildTS(year, month, 1, 0, 0);
        Long end = AppGlobal.buildTS(year, month, Month.of(month).maxLength(), 23, 59);
        return query.select(sql.formatted(AStorage.TABLE_NOTES)).params(start, end).listResult(mapper);
    }
    public List<Entities.Notes> findByTagId(Long tagId){
        var sql = "SELECT * FROM %s WHERE folderId = ? ;";
        return query.select(sql.formatted(AStorage.TABLE_NOTES)).params(tagId).listResult(mapper);
    }*/
}
