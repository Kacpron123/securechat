package org.project.securechat.server.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.project.securechat.sharedClass.Message;
import org.project.securechat.sharedClass.Message.DataType;
/**
 * This class provides utility methods for performing SQL operations related to the 'messages' table.
 * It encapsulates the database interactions for storing, retrieving, and managing chat messages.
 *
 */
public class SqlHandlerMessages extends BaseSqlServer{
 /** The 'messages' table is structured to store individual chat messages and contains the following columns:
 * <ul>
 * <li>{@code id} (INTEGER PRIMARY KEY AUTOINCREMENT): A unique, auto-generated identifier for each message.</li>
 * <li>{@code sender_id} (INTEGER NOT NULL): The identifier of the user who sent the message. This column typically
 * references the {@code user_id} in the 'friends' (or 'users') table, establishing a foreign key relationship.</li>
 * <li>{@code chat_id} (INTEGER NOT NULL): The identifier of the chat conversation to which the message belongs. This column
 * typically references the {@code chat_id} in the 'chats' table, establishing a foreign key relationship.</li>
 * <li>{@code type} (VARCHAR(50) NOT NULL): Defines the nature or category of the message content (e.g., "TEXT", "IMAGE", "FILE").
 * This type is typically mapped to an enumeration, such as {@link org.project.securechat.sharedClass.Message}.</li>
 * <li>{@code data} (TEXT NOT NULL): The actual content of the message. For text messages, this would be the message string.
 * For other types, it might store file paths, URLs, or serialized data.</li>
 * <li>{@code timestamp} (DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP): The precise date and time when the message was sent or recorded.
 * This column usually defaults to the current system timestamp upon insertion.</li>
 * </ul>
 *
 */
  public static void createMessagesTable() {
   String sql = "CREATE TABLE IF NOT EXISTS messages (" +
    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
    "sender_id INTEGER NOT NULL," +
    "chat_id INTEGER NOT NULL," +
    "type VARCHAR(15) NOT NULL, "+ //maybe containing as enum.ordinal() (?)
    "data TEXT," +
    "timestamp TEXT NOT NULL"
     +");";


    try (Connection conn = connect();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
      LOGGER.info("Table 'messages' has been created (if it did not exist).");
    } catch (SQLException e) {
      LOGGER.error("Error while creating table 'messages': {}", e.getMessage(), e);
    }
  }
  
/**
 * Inserts message into database.
 * 
 * @param mess Message object to be inserted
 */
public static void insertMessage(Message mess) {
    String sql = "INSERT INTO messages (" +
                 "sender_id, chat_id, type, data, " +
                 "timestamp" +
                 ") VALUES (?, ?, ?, ?, ?)";
  
    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setLong(1, mess.getSenderID());

        pstmt.setLong(2, mess.getChatID());
        pstmt.setString(3, mess.getDataType().toString());
        pstmt.setString(4, mess.getData());

        pstmt.setString(5, mess.getTimestamp().toString());

        pstmt.executeUpdate();

    } catch (SQLException e) {
        LOGGER.error("Error while inserting message into database: {}", e.getMessage(), e);
    }
}
/**
 * Retrieves list of messages sent to user after specified login date.
 * 
 * @param id The unique identifier of the user
 * @return List of messages sent to user after last login
 */
public static List<Message> getOlderMessages(Long id){
  //chincking server he is in:
  List<Message> messages = new ArrayList<>();
  String sql = "SELECT m.sender_id, m.chat_id,type, m.data, m.timestamp " +
    "FROM messages m " +
    "INNER JOIN chat_participant cp ON m.chat_id = cp.chat_id " +
    "WHERE cp.user_id = ? " +
    "AND m.timestamp > (SELECT last_login_time FROM users WHERE user_id = ?)" +
    "ORDER BY m.timestamp ASC";
  try (Connection conn = connect();
    PreparedStatement pstmt = conn.prepareStatement(sql)) {

    pstmt.setLong(1, id);
    pstmt.setLong(2, id);

    ResultSet rs = pstmt.executeQuery();

    while (rs.next()) {
        long senderId = rs.getLong("sender_id");
        long chatID = rs.getLong("chat_id");
        String data = rs.getString("data");
        DataType type=DataType.valueOf(rs.getString("type"));
        String timestamp = rs.getString("timestamp");

        Message msg = new Message(senderId, chatID, type, data, timestamp);
        messages.add(msg);
    }

} catch (SQLException e) {
    LOGGER.error("Error while retrieving older messages for user {}: {}", id, e.getMessage(), e);
  }
      return messages;
}

}
