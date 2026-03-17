package org.arepo;

import org.codejargon.fluentjdbc.api.FluentJdbcBuilder;
import org.codejargon.fluentjdbc.api.query.Query;

import java.sql.SQLException;

import static org.arepo.ASettings.*;

public class DBStorage {

    Query query;
    public DBStorage(String path) {
        var dataSource = new org.sqlite.SQLiteDataSource();
        dataSource.setUrl(path);
        var fluentJdbc = new FluentJdbcBuilder()
                .connectionProvider(dataSource)
                .build();
        query = fluentJdbc.query();

    }
    public void createTables() {
        String createProjects = """
            CREATE TABLE IF NOT EXISTS %s(
            	id INTEGER PRIMARY KEY,
            	title TEXT NOT NULL unique,
            	userId INTEGER NOT NULL,
            	statuses JSON NOT NULL
            );
            """;
        String createUsers = """
            CREATE TABLE IF NOT EXISTS %s (
            	id INTEGER PRIMARY KEY,
            	nickname TEXT NOT NULL unique,
            	email TEXT NOT NULL unique,
            	password TEXT,
            	token TEXT,
            	status INTEGER NOT NULL DEFAULT 0,
            	icon TEXT NOT NULL DEFAULT '',
            	created INTEGER,
            	projectId INTEGER,
            	settings JSON
            );
            """;

        query.update(createUsers.formatted(TABLE_USERS)).run();
        query.update(createProjects.formatted(TABLE_PROJECTS)).run();
    }

}
