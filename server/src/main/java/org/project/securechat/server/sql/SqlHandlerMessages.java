package org.project.securechat.server.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
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
    "sender_id VARCHAR(50) NOT NULL," +
    "chat_id VARCHAR(100) NOT NULL," +
    "data TEXT NOT NULL," +
    "timestamp DATETIME NOT NULL,"+
    "status VARCHAR(20) NOT NULL DEFAULT 'unread'"
     +");";


    try (Connection conn = connect();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
      System.out.println("Tabela 'users' została utworzona (jeśli nie istniała).");
    } catch (SQLException e) {
      System.err.println("Błąd podczas tworzenia tabeli: " + e.getMessage());
    }
  }
public static void insertMessage(String senderId, String receiverId,
                                 String data, 
                                 String timestamp) {
    String sql = "INSERT INTO messages (" +
                 "sender_id,  chat_id, data, " +
                 "timestamp" +
                 ") VALUES (?, ?, ?, ?)";
    String[] chatId = {senderId,receiverId};
  
 
    Arrays.sort(chatId);
    String chatID = String.join(":",chatId);
   
    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, senderId);

        pstmt.setString(2, chatID);
        pstmt.setString(3, data);

        pstmt.setString(4, timestamp); // np. Instant.now().toString()

        pstmt.executeUpdate();

    } catch (SQLException e) {
        e.printStackTrace();
    }
}
public static List<Message> getMessages(String chatId) {
    List<Message> messages = new ArrayList<>();
    String sql = "SELECT sender_id, chat_id, data, timestamp FROM messages WHERE chat_id = ? ORDER BY timestamp ASC";

    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, chatId);

        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            String senderId = rs.getString("sender_id");
            String chatID = rs.getString("chat_id");
            String data = rs.getString("data");
            String timestamp = rs.getString("timestamp");

            Message msg = new Message(senderId,chatID,DataType.TEXT, data,timestamp);
            messages.add(msg);
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }

    return messages;
}

}
