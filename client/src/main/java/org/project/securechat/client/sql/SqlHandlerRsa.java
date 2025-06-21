package org.project.securechat.client.sql;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlHandlerRsa  extends BaseSQL  {

  public static void createRsaTable() {
    String sql = "CREATE TABLE IF NOT EXISTS friends (" +
      "user_id INTEGER PRIMARY KEY, "+
      "login VARCHAR(50) UNIQUE NOT NULL," +
      "rsa_public_key TEXT" +
      ");";

    try (Connection conn = connect();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
      System.out.println("Tabela 'friends' została utworzona (jeśli nie istniała).");
    } catch (SQLException e) {
      System.err.println("Błąd podczas tworzenia tabeli 'friends': " + e.getMessage());
    }
  }
  
  public static String getRsaKey(long id){
    String sql = "SELECT * FROM friends WHERE user_id = ?";
    String rsaKey = null;
     try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setLong(1, id);
        ResultSet rs = pstmt.executeQuery();
        
        if (rs.next()) {
            rsaKey =  rs.getString("rsa_public_key");
            
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
     return rsaKey;
  }
  public static boolean insertFriend(long id,String login, String rsaPublicKey) {
      String sql = "INSERT OR IGNORE INTO friends (user_id, login, rsa_public_key) VALUES (?, ?, ?)";

      try (Connection conn = connect();
          PreparedStatement pstmt = conn.prepareStatement(sql)) {

          pstmt.setLong(1, id);
          pstmt.setString(2, login);
          pstmt.setString(3, rsaPublicKey);

          int affectedRows = pstmt.executeUpdate();
          return affectedRows == 1; // true jeśli dodano, false jeśli już był

      } catch (SQLException e) {
          e.printStackTrace();
          return false;
      }
  }
  public static boolean checkIfUserIdExists(long userID){
    String sql = "SELECT * FROM friends WHERE user_id = ?";
    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setLong(1, userID);
        ResultSet rs = pstmt.executeQuery();
        
        return rs.next();

    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
  }
  public static String getLogin(long userID){
    String sql = "SELECT login FROM friends WHERE user_id = ?";
    String login = null;
     try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setLong(1, userID);
        ResultSet rs = pstmt.executeQuery();
        
        if (rs.next()) {
            login =  rs.getString("login");
            
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
     return login;
}
  public static boolean changeLogin(long userID,String login){
    String sql = "UPDATE friends SET login = ? WHERE user_id = ?";
    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, login);
        pstmt.setLong(2, userID);
        int affectedRows = pstmt.executeUpdate();
        return affectedRows == 1; // true jeśli dodano, false jeśli już był

    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
  }
  public static long getUserId(String login){
    String sql = "SELECT user_id FROM friends WHERE login = ?";
    long userId = -1;
     try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, login);
        ResultSet rs = pstmt.executeQuery();
        
        if (rs.next()) {
            userId =  rs.getLong("user_id");
            
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
    return userId;
  }
}
 
