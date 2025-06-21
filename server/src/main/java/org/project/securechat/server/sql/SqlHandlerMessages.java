package org.project.securechat.server.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.project.securechat.sharedClass.Message;
import org.project.securechat.sharedClass.Message.DataType;
public class SqlHandlerMessages {
  private static final String DB_URL = "jdbc:sqlite:securechat.db"; // Nazwa pliku bazy danych
 
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
   public static void createMessagesTable() {
   String sql = "CREATE TABLE IF NOT EXISTS messages (" +
    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
    "sender_id INTEGER NOT NULL," +
    "chat_id INTEGER NOT NULL," +
    "data TEXT NOT NULL," +
    "timestamp DATETIME NOT NULL"
     +");";


    try (Connection conn = connect();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
      System.out.println("Tabela 'messages' została utworzona (jeśli nie istniała).");
    } catch (SQLException e) {
      System.err.println("Błąd podczas tworzenia tabeli 'messages': " + e.getMessage());
    }
  }
public static void insertMessage(long senderId, long  chatId,
                                 String data, 
                                 String timestamp) {
    String sql = "INSERT INTO messages (" +
                 "sender_id,  chat_id, data, " +
                 "timestamp" +
                 ") VALUES (?, ?, ?, ?)";
  
    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setLong(1, senderId);

        pstmt.setLong(2, chatId);
        pstmt.setString(3, data);

        pstmt.setString(4, timestamp); // np. Instant.now().toString()

        pstmt.executeUpdate();

    } catch (SQLException e) {
        e.printStackTrace();
    }
}
/**
 * Retrieves list of messages sent to user after specified login date.
 * 
 * @param id The unique identifier of the user
 * @param loginDate Login date of the user
 * @return List of messages sent to user after loginDate
 */
public static List<Message> getOlderMessages(Long id,LocalDateTime loginDate){
  //chincking server he is in:
  List<Message> messages = new ArrayList<>();
  String sql = "SELECT m.sender_id, m.chat_id, m.data, m.timestamp " +
    "FROM messages m " +
    "INNER JOIN chat_participant cp ON m.chat_id = cp.chat_id " +
    "WHERE cp.user_id = ? AND m.timestamp > ? " +
    "ORDER BY m.timestamp ASC";
  try (Connection conn = connect();
    PreparedStatement pstmt = conn.prepareStatement(sql)) {

    pstmt.setLong(1, id);
    pstmt.setString(2, loginDate.toString());

    ResultSet rs = pstmt.executeQuery();

    while (rs.next()) {
        long senderId = rs.getLong("sender_id");
        long chatID = rs.getLong("chat_id");
        String data = rs.getString("data");
        String timestamp = rs.getString("timestamp");

        Message msg = new Message(senderId, chatID, DataType.TEXT, data, timestamp);
        messages.add(msg);
    }

} catch (SQLException e) {
    e.printStackTrace();
  }
      return messages;
}

}
