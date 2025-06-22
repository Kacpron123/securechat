package org.project.securechat.server.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base class for all SQL operations.
 * 
 * Provides a common interface for establishing a database connection.
 */
public abstract class BaseSQL {
    protected static final Logger LOGGER = LogManager.getLogger();
    protected static final String DB_URL = "jdbc:sqlite:securechat.db";
    
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
