package org.project.securechat.server.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
public class SqlHandlerPasswords {

  private static final String DB_URL = "jdbc:sqlite:securechat.db"; // Nazwa pliku bazy danych

  public static void main(String[] args) {
    createUsersTable();
    System.out.println("Operacje zakończone. Sprawdź plik loginDB.db");
  }

  // Metoda do łączenia się z bazą danych
  private static Connection connect() {
    Connection conn = null;
    try {
      conn = DriverManager.getConnection(DB_URL);
      System.out.println("Połączono z bazą danych SQLite.");
    } catch (SQLException e) {
      System.err.println("Błąd połączenia z bazą danych: " + e.getMessage());
    }
    return conn;
  }

  // Metoda do tworzenia tabeli
  public static void createUsersTable() {
    String sql = "CREATE TABLE IF NOT EXISTS users (" +
        "user_id INTEGER PRIMARY KEY AUTOINCREMENT," +
        "username VARCHAR(50) UNIQUE NOT NULL," +
        "password VARCHAR(50) NOT NULL," + //for now is not hash for chcecking
        "rsa_public_key TEXT, "+ // 'data' jako TEXT dla daty
        "last_login_time DATETIME"+ // 
        ");";

    try (Connection conn = connect();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
      System.out.println("Tabela 'users' została utworzona (jeśli nie istniała).");
    } catch (SQLException e) {
      System.err.println("Błąd podczas tworzenia tabeli: " + e.getMessage());
    }
  }
  public static boolean updateLastLoginTime(String username, LocalDateTime lastLoginTime) {
    String sql = "UPDATE users SET last_login_time = ? WHERE username = ?";
    try (Connection conn = connect();
      PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, lastLoginTime.toString());
      pstmt.setString(2, username);
      
      int rowsAffected = pstmt.executeUpdate();
      return rowsAffected > 0;
    } catch (SQLException e) {
      System.err.println("Błąd podczas aktualizacji czasu ostatniej logowania dla użytkownika '" + username + "': " + e.getMessage());
      return false;
    }
  }

  // Metoda do wstawiania danych
  public static boolean insertUser(String username, String password) {
    String sql = "INSERT INTO users(username, password,rsa_public_key,last_login_time) VALUES(?,?,?,?)";

    try (Connection conn = connect();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, username);
      pstmt.setString(2, password);
      pstmt.setNull(3, java.sql.Types.VARCHAR);
      pstmt.setString(4, LocalDateTime.now().toString());
      int rowsAffected = pstmt.executeUpdate();
      return rowsAffected == 1; // Jeśli dodano 1 wiersz, to sukces
    } catch (SQLException e) {
      System.err.println("Błąd podczas wstawiania użytkownika '" + username + "': " + e.getMessage());
      return false;
    }
  }
  
  public static String getUsernameFromUserId(long userId) {
    String sql = "SELECT username FROM users WHERE user_id = ?";
    try (Connection conn = connect();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setLong(1, userId);
      ResultSet rs = pstmt.executeQuery();

      if (rs.next()) {
        return rs.getString("username");
      }
    } catch (SQLException e) {
      System.err.println("Błąd podczas pobierania nazwy użytkownika o ID=" + userId + ": " + e.getMessage());
    }
    return null;
  }
  public static boolean updateKey(String username, String rsaKey) {
    String selectSql = "SELECT rsa_public_key FROM users WHERE username = ?";
    String updateSql = "UPDATE users SET rsa_public_key = ? WHERE username = ?";

    try (Connection conn = connect();
         PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {

        selectStmt.setString(1, username);
        ResultSet rs = selectStmt.executeQuery();

        if (rs.next()) {
            String existingKey = rs.getString("rsa_public_key");
            if (existingKey != null) {
                // Klucz już istnieje — nie aktualizujemy
                return false;
            }
        } else {
            // Nie znaleziono użytkownika
            return false;
        }

        // Klucz nie istnieje — aktualizujemy
        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
            updateStmt.setString(1, rsaKey);
            updateStmt.setString(2, username);
            updateStmt.executeUpdate();
            return true;
        }

    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
  }
  public static String getPublicKey(long userId) {
    String sql = "SELECT rsa_public_key FROM users WHERE user_id = ?";

    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setLong(1, userId);
        ResultSet rs = pstmt.executeQuery();

        if (rs.next()) {
            return rs.getString("rsa_public_key"); // Może być null, jeśli nie ustawiono
        } else {
            return null; // Użytkownik nie istnieje
        }

    } catch (SQLException e) {
        e.printStackTrace();
        return null;
    }
}

  public static String getUserPassword(String username) {
    String sql = "SELECT password FROM users WHERE username = ?";
    String password = null;

    try (Connection conn = connect();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, username);
      ResultSet rs = pstmt.executeQuery();

      if (rs.next()) {
        password = rs.getString("password");
      }
    } catch (SQLException e) {
      System.err.println("Błąd podczas pobierania hasła dla użytkownika '" + username + "': " + e.getMessage());
    }
    return password;
  }
  /**
   * Retrieves the user ID for the specified username.
   *
   * @param username the username for which to retrieve the user ID
   * @return the user ID if the username exists in the database, or -1 if not found
   */
  public static long getUserId(String username){
    String sql="SELECT user_id from users WHERE username = ?";
    try(PreparedStatement ps=connect().prepareStatement(sql)){
      ps.setString(1, username);
      try(ResultSet rs=ps.executeQuery()){
        if(rs.next())
          return rs.getLong("user_id");
      }
    }catch(SQLException e){
      System.err.println("Błąd podczas sprawdzania użytkownika '"+username+"' "+e.getMessage());
    }
    return -1;
  }
/**
 * Drops a table from the database if it exists.
 *
 * @param tableName the name of the table to be dropped
 */

  public static void dropTable(String tableName) {
    // DROP TABLE statement with IF EXISTS clause to avoid an error if the table does not exist.
    String sql = "DROP TABLE IF EXISTS " + tableName;

    try (Connection conn = connect();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
      System.out.println("Tabela '" + tableName + "' została usunięta (jeśli istniała).");
    } catch (SQLException e) {
      System.err.println("Błąd podczas usuwania tabeli '" + tableName + "': " + e.getMessage());
    }
  }
}