package org.project.securechat.client.sql;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SqlHandlerConversations extends BaseSQL {

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
  public static void createChatRelated(){
    String sqlchat = "CREATE TABLE IF NOT EXISTS chats (" +
      "chat_id INTEGER PRIMARY KEY," +
      "chat_name VARCHAR(50) NOT NULL ," +
      "is_group_chat BOOLEAN DEFAULT FALSE NOT NULL ," +
      "AES_KEY VARCHAR(250) NOT NULL" +
      ");";
    String sqlfriend = "CREATE TABLE IF NOT EXISTS chat_member (" +
      "chat_id INTEGER NOT NULL," +
      "user_id INTEGER NOT NULL, " +
      "PRIMARY KEY  (chat_id, user_id), "+
      "FOREIGN KEY (chat_id) REFERENCES chats(chat_id) ON DELETE CASCADE, "+
      "FOREIGN KEY (user_id) REFERENCES friends(user_id) ON DELETE CASCADE"+
      ");";

    try (Connection conn = connect();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sqlchat);
      stmt.execute(sqlfriend);
      System.out.println("Tabele 'chat' i 'chat_member' zostały utworzone (jeśli nie istniały).");
    } catch (SQLException e) {
      System.err.println("Błąd podczas tworzenia tabel: " + e.getMessage());
      e.printStackTrace();
    }
  }
  // public static void createChat(long id, )
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
  static public long chat_2_Exist(String username){
    String sql = "SELECT chat_id FROM chats WHERE chat_name = ?";
    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, username);
        try (ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("chat_id");
            }
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
    return 0;
  }
  static public void Create_2_chat(long chat_id,long myId,long userId,String aes_key){
    String sql = "INSERT INTO chats (chat_id, chat_name, AES_KEY) VALUES (?, ?, ?)";
    String username = SqlHandlerRsa.getLogin(userId);
    try (Connection conn = connect();

        PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setLong(1, chat_id);
        pstmt.setString(2, username);
        pstmt.setString(3, aes_key);
        pstmt.executeUpdate();

    } catch (SQLException e) {
        e.printStackTrace();
    }
    String sqlMember = "INSERT INTO chat_member (chat_id, user_id) VALUES (?, ?)";
    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sqlMember)) {

        pstmt.setLong(1, chat_id);
        pstmt.setLong(2, userId);
        pstmt.executeUpdate();

    } catch (SQLException e){
        e.printStackTrace();
    }
  }
  static public String getaesKey(long chat_id){
    String sql = "SELECT AES_KEY FROM chats WHERE chat_id = ?";
    try (Connection conn = connect();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setLong(1, chat_id);
        try (ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getString("AES_KEY");
            }
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
    return null;
  }

}
