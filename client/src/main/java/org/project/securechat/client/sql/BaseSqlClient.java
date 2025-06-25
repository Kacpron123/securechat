package org.project.securechat.client.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base class for all SQL operations.
 * 
 * Provides a common interface for establishing a database connection.
 */
public abstract class BaseSqlClient {
    /**
     * The logger for this class.
     */
    protected static final Logger LOGGER = LogManager.getLogger();
    /**
     * The URL for the SQLite database.
     */
    protected static final String DB_URL = "jdbc:sqlite:client_database.db";
    /**
     * default constructor for the BaseSQL class.
     */
    BaseSqlClient(){
        // TODO change into client[ID]_database.db
    }
    /**
     * Establishes a connection to the SQLite database.
     * @return A connection to the database
     * @throws SQLException If the connection fails
     */
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
    /**
     * Main method for printing the contents of all tables in the database
     * @param args not used
     */
    public static void main(String[] args) {
      Connection conn = null;
      try {
          // Establish a connection to the database
          conn = DriverManager.getConnection(DB_URL);
          LOGGER.info("Connected to SQLite database: {}", DB_URL);

          // --- Logic from getAllTableNames implemented directly ---
          List<String> tableNames = new ArrayList<>();
          String sqlGetTables = "SELECT name FROM sqlite_master WHERE type='table';";

          try (Statement stmtTables = conn.createStatement();
               ResultSet rsTables = stmtTables.executeQuery(sqlGetTables)) {
              while (rsTables.next()) {
                  tableNames.add(rsTables.getString("name"));
              }
          }
          // --- End of logic from getAllTableNames ---

          if (tableNames.isEmpty()) {
              LOGGER.info("No tables in the database.");
          } else {
              LOGGER.info("\n--- Contents of all tables ---");
              for (String tableName : tableNames) {
                  LOGGER.info("\n--- Table: {} ---", tableName);

                  // --- Logic from displayTableContent implemented directly ---
                  String sqlDisplayContent = "SELECT * FROM " + tableName;

                  try (Statement stmtContent = conn.createStatement();
                       ResultSet rsContent = stmtContent.executeQuery(sqlDisplayContent)) {

                      ResultSetMetaData rsmd = rsContent.getMetaData();
                      int columnCount = rsmd.getColumnCount();

                      // Display the headers of the columns
                      for (int i = 1; i <= columnCount; i++) {
                          LOGGER.info(" {} ", rsmd.getColumnName(i)); // Format for better readability
                      }
                      LOGGER.info("\n-----------------------------------------------------------");

                      // Display the data
                      while (rsContent.next()) {
                          for (int i = 1; i <= columnCount; i++) {
                              LOGGER.info(" {} ", rsContent.getObject(i));
                          }
                          LOGGER.info("\n");
                      }
                  }
                  // --- End of logic from displayTableContent ---
              }
          }

        } catch (SQLException e) {
          // Use LOGGER to print errors
          LOGGER.error("Error during database operations: {}", e.getMessage());
          // You can also print the stack trace for more detailed information
          e.printStackTrace();
        } finally {
          // Close the connection
          try {
              if (conn != null) {
                  conn.close();
                  LOGGER.info("\nConnection to the database closed.");
              }
          } catch (SQLException ex) {
              LOGGER.error("Error during closing connection: {}", ex.getMessage());
          }
        }
    }
    /**
     * drop table
     * @param tableName name
     */
    public static void dropTable(String tableName) {
        // Instrukcja DROP TABLE. Klauzula IF EXISTS zapobiega błędowi, jeśli tabela nie
        // istnieje.
        String sql = "DROP TABLE IF EXISTS " + tableName;
    
        try (Connection conn = DriverManager.getConnection(DB_URL);
            Statement stmt = conn.createStatement()) {
          stmt.execute(sql);
          System.out.println("Tabela '" + tableName + "' została usunięta (jeśli istniała).");
        } catch (SQLException e) {
          System.err.println("Błąd podczas usuwania tabeli '" + tableName + "': " + e.getMessage());
        }
    }
    // public createTable ?
}
