package org.project.securechat.server.sql;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used to handle SQL operations related to conversations.
 */
public class SqlHandlerConversations extends BaseSqlServer{
/**
 * Creates the necessary database tables for managing conversations and their participants.
 *
 * The 'chats' table stores general information about individual chat conversations:
 * <ul>
 * <li>{@code chat_id} (INTEGER PRIMARY KEY): A unique identifier for the chat.</li>
 * <li>{@code chat_name} (VARCHAR(50) NOT NULL): The display name of the chat.</li>
 * <li>{@code is_group_chat} (BOOLEAN DEFAULT FALSE NOT NULL): A flag indicating if the chat is a group conversation. Defaults to false (private chat).</li>
 * </ul>
 *
 * The 'chat_participant' table manages the many-to-many relationship between chats and users,
 * and stores the AES key encrypted specifically for each participant:
 * <ul>
 * <li>{@code chat_id} (INTEGER NOT NULL): The identifier of the chat, referencing the 'chats' table.</li>
 * <li>{@code user_id} (INTEGER NOT NULL): The identifier of the user, referencing the 'friends' (or 'users') table.</li>
 * <li>{@code encrypted_aes_key} (VARCHAR(512) NOT NULL): The symmetric AES key for the chat, encrypted using the participant's individual RSA public key.
 * This key is specific to the {@code chat_id} and {@code user_id} pair.</li>
 * </ul>
 * The primary key of the 'chat_participant' table is a composite key of ({@code chat_id}, {@code user_id}),
 * ensuring each user can only participate once in a given chat and stores one unique encrypted key.
 */
  public static void createChatRelated() {
    String sqlchat = "CREATE TABLE IF NOT EXISTS chats (" +
        "chat_id INTEGER PRIMARY KEY AUTOINCREMENT," +
        "chat_name VARCHAR(50) NOT NULL," +
        "is_group_chat BOOLEAN DEFAULT FALSE NOT NULL"+
        ");";

    String sqlparticipant = "CREATE TABLE IF NOT EXISTS chat_participant (" +
        "chat_id INTEGER NOT NULL," +
        "user_id INTEGER NOT NULL," +
        "encrypted_aes_key VARBINARY(250) NOT NULL," + 
        "PRIMARY KEY (chat_id,user_id)," +
        "FOREIGN KEY (chat_id) REFERENCES chats(chat_id) ON DELETE CASCADE ON UPDATE CASCADE,"+
        "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE"+
        ");";

    try (Connection conn = connect();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sqlchat);
      stmt.execute(sqlparticipant);
      LOGGER.debug("Table 'chats' created (if not already existing).");
    } catch (SQLException e) {
      LOGGER.error("Error creating table 'chat_participant': {}", e.getMessage());
    }
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
        // Sort user IDs and encrypted AES keys by user ID
        if (user1_id > user2_id) {
            long temp = user1_id;
            user1_id = user2_id;
            user2_id = temp;

            String tempAes = aes1;
            aes1 = aes2;
            aes2 = tempAes;
        }
        
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
                    LOGGER.error("Error during rollback: " + excep.getMessage());
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
                    LOGGER.error("Error closing connection: " + e.getMessage());
                }
            }
        }
        return newChatId;
    }
  // TODO group chat:
  // public static void createGroupChat(){}
  // public static void insertUsertoChat(){}


  /**
   * Gets all users from the chat with the given ID, excluding the user with the given ID.
   * 
   * @param chatId The ID of the chat.
   * @param userId The ID of the user to exclude.
   * @return A list of IDs of users in the chat, excluding the user with the given ID.
   */
  public static List<Long> getUsersFromChat(long chatId, long userId) {
        String selectPeopleFromChatSql = "SELECT user_id FROM chat_participant WHERE chat_id = ? AND user_id <> ?";
        List<Long> people = new ArrayList<>();
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(selectPeopleFromChatSql)) {
            pstmt.setLong(1, chatId);
            pstmt.setLong(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    people.add(rs.getLong("user_id"));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error getting people from chat: " + e.getMessage());
        }
        return people;
    }
}
