package org.project.securechat.client.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SQLiteTableViewer {

  private static final String DB_URL = "jdbc:sqlite:client_database.db"; // Ścieżka do Twojej bazy danych SQLite

  public static void main(String[] args) {
    Connection conn = null;
    try {
      // Połączenie z bazą danych
      conn = DriverManager.getConnection(DB_URL);
      System.out.println("Połączono z bazą danych SQLite: " + DB_URL);

      // Pobierz nazwy wszystkich tabel
      List<String> tableNames = getAllTableNames(conn);

      if (tableNames.isEmpty()) {
        System.out.println("Brak tabel w bazie danych.");
      } else {
        System.out.println("\n--- Zawartość wszystkich tabel ---");
        for (String tableName : tableNames) {
          System.out.println("\n--- Tabela: " + tableName + " ---");
          displayTableContent(conn, tableName);
        }
      }

    } catch (SQLException e) {
      System.err.println("Błąd podczas operacji na bazie danych: " + e.getMessage());
    } finally {
      // Zamknięcie połączenia
      try {
        if (conn != null) {
          conn.close();
          System.out.println("\nPołączenie z bazą danych zamknięte.");
        }
      } catch (SQLException ex) {
        System.err.println("Błąd podczas zamykania połączenia: " + ex.getMessage());
      }
    }
  }

  /**
   * Pobiera nazwy wszystkich tabel w bazie danych.
   * 
   * @param conn Aktywne połączenie z bazą danych.
   * @return Lista nazw tabel.
   * @throws SQLException Jeśli wystąpi błąd SQL.
   */
  private static List<String> getAllTableNames(Connection conn) throws SQLException {
    List<String> tableNames = new ArrayList<>();
    // Zapytanie do specjalnej tabeli SQLite, która przechowuje informacje o
    // schemacie
    String sql = "SELECT name FROM sqlite_master WHERE type='table';";

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {
      while (rs.next()) {
        tableNames.add(rs.getString("name"));
      }
    }
    return tableNames;
  }

  /**
   * Wyświetla zawartość podanej tabeli.
   * 
   * @param conn      Aktywne połączenie z bazą danych.
   * @param tableName Nazwa tabeli do wyświetlenia.
   * @throws SQLException Jeśli wystąpi błąd SQL.
   */
  private static void displayTableContent(Connection conn, String tableName) throws SQLException {
    String sql = "SELECT * FROM " + tableName; // Wybierz wszystkie kolumny z tabeli

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      ResultSetMetaData rsmd = rs.getMetaData();
      int columnCount = rsmd.getColumnCount();

      // Wyświetl nagłówki kolumn
      for (int i = 1; i <= columnCount; i++) {
        System.out.printf("%-20s", rsmd.getColumnName(i)); // Formatowanie dla lepszej czytelności
      }
      System.out.println("\n-----------------------------------------------------------");

      // Wyświetl dane
      while (rs.next()) {
        for (int i = 1; i <= columnCount; i++) {
          System.out.printf("%-20s", rs.getObject(i));
        }
        System.out.println();
      }
    }
  }

}
