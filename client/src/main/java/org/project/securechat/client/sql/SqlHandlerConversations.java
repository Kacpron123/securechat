package org.project.securechat.client.sql;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Class used to handle SQL operations related to conversations.
 *
 */
public class SqlHandlerConversations extends BaseSQL {


  /**
 * Creates the necessary database tables for managing conversations and their members.
 *
 * <p>
 * The 'chats' table stores information about individual chat conversations:
 * <ul>
 * <li>{@code chat_id} (INTEGER PRIMARY KEY): A unique identifier for the chat.</li>
 * <li>{@code chat_name} (VARCHAR(50) NOT NULL): The name of the chat.</li>
 * <li>{@code is_group_chat} (BOOLEAN DEFAULT FALSE NOT NULL): A flag indicating if the chat is a group conversation.</li>
 * <li>{@code AES_KEY} (VARCHAR(250) NOT NULL): The AES key used for encrypting messages within the chat.</li>
 * </ul>
 * </p>
 *
 * <p>
 * The 'chat_member' table manages the many-to-many relationship between chats and users:
 * <ul>
 * <li>{@code chat_id} (INTEGER NOT NULL): An identifier for the chat, referencing the 'chats' table.</li>
 * <li>{@code user_id} (INTEGER NOT NULL): An identifier for the user, referencing the 'friends' (or 'users') table.</li>
 * </ul>
 * The primary key of the 'chat_member' table is a composite key of ({@code chat_id}, {@code user_id}).
 * </p>
 */
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


  /**
   * Checks if a chat with the given name exists.
   * 
   * @param name The name to search for.
   * @return The chat_id of the chat with the given name or -1 if it doesn't exist.
   */
  static public long chat_2_Exist(String name){
    String sql = "SELECT chat_id FROM chats WHERE chat_name = ?";
    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, name);
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

  /**
   * Creates a new one-to-one chat in the database.
   * 
   * @param chat_id The ID of the chat to create.
   * @param myId The ID of the current user.
   * @param userId The ID of the other user.
   * @param aes_key The AES key to use for encrypting messages in this chat.
   * @throws SQLException If a database access error occurs.
   */
  static public void Create_2_chat(long chat_id,long myId,long userId,String aes_key){
    String sql = "INSERT INTO chats (chat_id, chat_name, AES_KEY) VALUES (?, ?, ?)";
    String username = SqlHandlerFriends.getLogin(userId);
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
  
  /**
   * Gets the name of the chat with the given ID.
   * 
   * @param chat_id The ID of the chat.
   * @return The name of the chat, or null if an error occurs.
   */
  static public String getName(long chat_id){
    String sql = "SELECT chat_name FROM chats WHERE chat_id = ?";
    try (Connection conn = connect();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setLong(1, chat_id);
        try (ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getString("chat_name");
            }
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
    return null;
  }
  
}
