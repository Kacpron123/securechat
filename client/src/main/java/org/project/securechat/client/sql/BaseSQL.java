package org.project.securechat.client.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BaseSQL {
    protected static final Logger LOGGER = LogManager.getLogger();
    // TODO change into client[ID]_database.db
    protected static final String DB_URL = "jdbc:sqlite:client_database.db";
    
    static Connection connect() throws SQLException {
    Connection conn = null;
    try {
        conn = DriverManager.getConnection(DB_URL);
        return conn;
    } catch (SQLException e) {
        LOGGER.fatal("Cannot connect to database {}: {}", DB_URL, e.getMessage(), e);
        throw e;
    }
    }
    // public createTable ?
}
