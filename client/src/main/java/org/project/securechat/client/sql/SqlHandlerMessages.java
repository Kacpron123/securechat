package org.project.securechat.client.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.project.securechat.sharedClass.Message;
/**
 * Class used to handle SQL operations related to messages.
 */
public class SqlHandlerMessages extends BaseSQL {

  
  /**
   * Creates the 'messages' table, designed to store individual chat messages.
   *
   * <p>
   * The 'messages' table contains the following columns:
   * <ul>
   * <li>{@code id} (INTEGER PRIMARY KEY AUTOINCREMENT): A unique identifier for each message. This is typically auto-generated.</li>
   * <li>{@code sender_id} (INTEGER NOT NULL): An identifier for the user who sent the message, referencing the 'friends' (or 'users') table.</li>
   * <li>{@code chat_id} (INTEGER NOT NULL): An identifier for the chat to which the message belongs, referencing the 'chats' table.</li>
   * <li>{@code type} (VARCHAR(50) NOT NULL): The type of the message (e.g., "text", "image", "file"). This allows for future extensibility.</li>
   * <li>{@code data} (TEXT NOT NULL): The actual content of the message. This can be large, so {@code TEXT} is often preferred over {@code VARCHAR} for message content.</li>
   * <li>{@code timestamp} (DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP): The exact date and time when the message was sent or received. Automatically set to the current time if not provided.</li>
   * </ul>
   * </p>
   *
   */
  public static void createMessagesTable() {
    String sql = "CREATE TABLE IF NOT EXISTS messages (" +
      "id INTEGER PRIMARY KEY AUTOINCREMENT," +
      "sender_id INTEGER NOT NULL," +
      "chat_id INTEGER NOT NULL," +
      // "type VARCHAR(15) NOT NULL, " + // later type for changes/text/file
      "data TEXT," +
      "timestamp TEXT NOT NULL"
      +");";

    try (Connection conn = connect();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
      System.out.println("Tabela 'messages' została utworzona (jeśli nie istniała).");
    } catch (SQLException e) {
      System.err.println("Błąd podczas tworzenia tabeli 'messages': " + e.getMessage());
    }
  }
  /**
   * Inserts message into database.
   * 
   * @param mess Message object to be inserted
   */
  public static void insertMessage(Message mess) {
    String sql = "INSERT INTO messages (" +
                 "sender_id, chat_id, data, " +
                 "timestamp" +
                 ") VALUES (?, ?, ?, ?)";
  
    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setLong(1, mess.getSenderID());

        pstmt.setLong(2, mess.getChatID());
        // pstmt.setString(3, mess.getDataType().toString());
        pstmt.setString(3, mess.getData());
        pstmt.setString(4, mess.getTimestamp().toString());

        pstmt.executeUpdate();

    } catch (SQLException e) {
        e.printStackTrace();
    }
  }
}
