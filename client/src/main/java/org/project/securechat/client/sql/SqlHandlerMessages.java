package org.project.securechat.client.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.project.securechat.sharedClass.Message;

public class SqlHandlerMessages extends BaseSQL {
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
