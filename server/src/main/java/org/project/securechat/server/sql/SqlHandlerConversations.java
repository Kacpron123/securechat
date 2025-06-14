package org.project.securechat.server.sql;
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
   private static final String DB_URL = "jdbc:sqlite:securechat.db";

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
      System.out.println("Tabela 'users' została utworzona (jeśli nie istniała).");
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
  private static void createChatTable() {
  String sql = "CREATE TABLE IF NOT EXISTS chats (" +
    "chat_id INTEGER PRIMARY KEY AUTOINCREMENT," +
    "chat_name VARCHAR(50) NOT NULL," +
    "is_group_chat BOOLEAN DEFAULT FALSE NOT NULL"+
    ");";

    try (Connection conn = connect();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
      System.out.println("Tabela 'chats' została utworzona (jeśli nie istniała).");
    } catch (SQLException e) {
      System.err.println("Błąd podczas tworzenia tabeli 'chats': " + e.getMessage());
    }
  }
  
  private static void createChatParticipantsTable() {
  String sql = "CREATE TABLE IF NOT EXISTS chat_participant (" +
    "chat_id INTEGER NOT NULL," +
    "user_id INTEGER NOT NULL," +
    "encrypted_aes_key VARBINARY(250) NOT NULL," + 
    "PRIMARY KEY (chat_id,user_id)," +
    "FOREIGN KEY (chat_id) REFERENCES chats(chat_id) ON DELETE CASCADE ON UPDATE CASCADE,"+
    "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE"+
    ");";

    try (Connection conn = connect();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
      System.out.println("Tabela 'chats' została utworzona (jeśli nie istniała).");
    } catch (SQLException e) {
      System.err.println("Błąd podczas tworzenia tabeli 'chat_participant': " + e.getMessage());
    }
  }
  public static void createChatRelated(){
    createChatParticipantsTable();
    createChatTable();
  }
    /**
     * Inserts a new one-to-one chat and adds both users as participants.
     *
     * @param user1_id The ID of the first user.
     * @param user2_id The ID of the second user.
     * @param aes1 The encrypted AES key for user1 in this chat.
     * @param aes2 The encrypted AES key for user2 in this chat.
     * @return The chat_id of the newly created chat.
     * @throws SQLException If a database access error occurs.
     */
    public static long insertOneToOneChat(long user1_id, long user2_id, String aes1, String aes2) throws SQLException {
        long newChatId = -1; // Initialize with an invalid ID
        String insertChatSql = "INSERT INTO chats (chat_name, is_group_chat) VALUES (?, ?)";

        // SQL for inserting into 'chat_participant' table
        String insertParticipantSql = "INSERT INTO chat_participant (chat_id, user_id, encrypted_aes_key) VALUES (?, ?, ?)";

        Connection conn = null; // Declare outside try-with-resources to manage transaction
        try {
            conn = connect();
            // Start a transaction to ensure atomicity
            // If any part fails, the whole operation rolls back
            conn.setAutoCommit(false);
            
            // 1. Insert into 'chats' table
            try (PreparedStatement pstmtChat = conn.prepareStatement(insertChatSql, Statement.RETURN_GENERATED_KEYS)) {
                // A one-to-one chat might not have a specific name initially,
                // or you could generate one like "User1 & User2 Chat"
                pstmtChat.setString(1, "" + user1_id + ":" + user2_id);
                pstmtChat.setBoolean(2, false); // This is a one-to-one chat, so is_group_chat is false

                int affectedRows = pstmtChat.executeUpdate();

                if (affectedRows == 0) {
                    throw new SQLException("Creating chat failed, no rows affected.");
                }

                // Retrieve the auto-generated chat_id
                try (ResultSet generatedKeys = pstmtChat.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        newChatId = generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Creating chat failed, no ID obtained.");
                    }
                }
            }

            // 2. Insert user1 into 'chat_participant'
            try (PreparedStatement pstmtUser1 = conn.prepareStatement(insertParticipantSql)) {
                pstmtUser1.setLong(1, newChatId);
                pstmtUser1.setLong(2, user1_id);
                // Convert AES key string to bytes (assuming it's base64 encoded or similar)
                // For demonstration, let's just use getBytes(). In production, handle encoding!
                pstmtUser1.setBytes(3, aes1.getBytes());
                pstmtUser1.executeUpdate();
            }

            // 3. Insert user2 into 'chat_participant'
            try (PreparedStatement pstmtUser2 = conn.prepareStatement(insertParticipantSql)) {
                pstmtUser2.setLong(1, newChatId);
                pstmtUser2.setLong(2, user2_id);
                // Convert AES key string to bytes (assuming it's base64 encoded or similar)
                // For demonstration, let's just use getBytes(). In production, handle encoding!
                pstmtUser2.setBytes(3, aes2.getBytes());
                pstmtUser2.executeUpdate();
            }

            // Commit the transaction if all operations succeed
            conn.commit();

        } catch (SQLException e) {
            // Rollback the transaction if any error occurs
            if (conn != null) {
                try {
                    System.err.print("Transaction is being rolled back");
                    conn.rollback();
                } catch (SQLException excep) {
                    System.err.print("Error during rollback: " + excep.getMessage());
                }
            }
            throw e; // Re-throw the original exception after rollback
        } finally {
            // Always close the connection in a finally block
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset auto-commit mode
                    conn.close();
                } catch (SQLException e) {
                    System.err.print("Error closing connection: " + e.getMessage());
                }
            }
        }
        return newChatId;
    }
  // TODO group chat:
  // public static void createGroupChat(){}
  // public static void insertUsertoChat(){}
  

}
