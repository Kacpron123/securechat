package org.project.securechat.client.sql;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SqlHandlerConversations {
   private static final String DB_URL = "jdbc:sqlite:client_database.db";

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
     public static void createConversationsTable() {
  String sql = "CREATE TABLE IF NOT EXISTS conversations (" +
    "chat_id VARCHAR(100) PRIMARY KEY," +
    "user1 VARCHAR(50) NOT NULL," +
    "user2 VARCHAR(50) NOT NULL," +
    "aes_key_for_user1 TEXT ," +
    "aes_key_for_user2 TEXT " +
");";

    try (Connection conn = connect();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
      System.out.println("Tabela 'conversations' została utworzona (jeśli nie istniała).");
    } catch (SQLException e) {
      System.err.println("Błąd podczas tworzenia tabeli: " + e.getMessage());
    }
  }
  public static void insertConversation( String user1, String user2,
                                      String aesKeyUser1, String aesKeyUser2) {
    String sql = "INSERT INTO conversations (chat_id, user1, user2, aes_key_for_user1, aes_key_for_user2) " +
                 "VALUES (?, ?, ?, ?, ?)";
     String[] chatId = {user1,user2};
  
 
    Arrays.sort(chatId);
    String chatID = String.join(":",chatId);              
    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, chatID);
        pstmt.setString(2, user1);
        pstmt.setString(3, user2);
        pstmt.setString(4, aesKeyUser1);
        pstmt.setString(5, aesKeyUser2);
        pstmt.executeUpdate();
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
public static Map<String, String> getConversation(String user1,String user2) {
  String[] chatId = {user1,user2};
  
 
    Arrays.sort(chatId);
    String chatID = String.join(":",chatId);  
    String sql = "SELECT * FROM conversations WHERE chat_id = ?";
    Map<String, String> result = new HashMap<>();

    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, chatID);
        ResultSet rs = pstmt.executeQuery();

        if (rs.next()) {
            result.put("chat_id", rs.getString("chat_id"));
            result.put("user1", rs.getString("user1"));
            result.put("user2", rs.getString("user2"));
            result.put("aes_key_for_user1", rs.getString("aes_key_for_user1"));
            result.put("aes_key_for_user2", rs.getString("aes_key_for_user2"));
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }

    return result.isEmpty() ? null : result;
}

}
