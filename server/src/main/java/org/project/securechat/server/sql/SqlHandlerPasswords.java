package org.project.securechat.server.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
public class SqlHandlerPasswords extends BaseSQL{

  /**
   * Creates the 'users' table to store user information, including RSA public keys.
   * 
   * <p>
   * The 'users' table contains the following columns:
   * <ul>
   * <li>{@code user_id} (INTEGER PRIMARY KEY AUTOINCREMENT): A unique numerical identifier for the user.</li>
   * <li>{@code username} (VARCHAR(50) UNIQUE NOT NULL): A unique login name for the user.</li>
   * <li>{@code password} (VARCHAR(50) NOT NULL): The password associated with the user, used for authentication. Note that this should be hashed and salted in a real application.</li>
   * <li>{@code rsa_public_key} (VARCHAR(2048) NOT NULL): The RSA public key associated with the user, used for secure communication.</li>
   * <li>{@code last_login_time} (TEXT NOT NULL): The time when the user last logged in.</li>
   * </ul>
   * </p>
   */
  public static void createUsersTable() {
    String sql = "CREATE TABLE IF NOT EXISTS users (" +
        "user_id INTEGER PRIMARY KEY AUTOINCREMENT," +
        "username VARCHAR(50) UNIQUE NOT NULL," +
        "password VARCHAR(50) NOT NULL," + //for now is not hash for chcecking
        "rsa_public_key TEXT, "+ // 'data' jako TEXT dla daty //TODO create as NOT NULL, registration without asking key
        "last_login_time TEXT"+ // 
        ");";

    try (Connection conn = connect();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
      LOGGER.info("Table 'users' has been created (if it did not exist).");
    } catch (SQLException e) {
      LOGGER.error("Error creating table: {}", e.getMessage());
    }
  }
  
  /**
   * Updates the last login time for the user with the specified ID.
   * 
   * @param id The ID of the user.
   * @param lastLoginTime The new last login time for the user.
   * @return true if the last login time was successfully updated, false otherwise.
   */
  public static boolean updateLastLoginTime(Long id, Instant lastLoginTime) {
    String sql = "UPDATE users SET last_login_time = ? WHERE user_id = ?";
    try (Connection conn = connect();
      PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, lastLoginTime.toString());
      pstmt.setLong(2, id);
      
      int rowsAffected = pstmt.executeUpdate();
      return rowsAffected > 0;
    } catch (SQLException e) {
      LOGGER.error("Error updating last login time for user {}: {}", id, e.getMessage());
      return false;
    }
  }

  /**
   * Inserts a new user into the database.
   * 
   * @param username The login name of the user to be inserted.
   * @param password The password of the user to be inserted.
   * @return true if the user is successfully inserted, false otherwise.
   */
  public static boolean insertUser(String username, String password) {
    String sql = "INSERT INTO users(username, password,rsa_public_key,last_login_time) VALUES(?,?,?,?)";

    try (Connection conn = connect();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, username);
      pstmt.setString(2, password);
      pstmt.setNull(3, java.sql.Types.VARCHAR);
      pstmt.setString(4, Instant.now().toString());
      int rowsAffected = pstmt.executeUpdate();
      return rowsAffected == 1; // Jeśli dodano 1 wiersz, to sukces
    } catch (SQLException e) {
      LOGGER.error("Error inserting user '{}' into the database: {}", username, e.getMessage());
      return false;
    }
  }
  
  /**
   * Retrieves the username associated with the user with the specified ID from the 'users' table.
   * 
   * @param userId The ID of the user to retrieve the username for.
   * @return The username associated with the user, or null if the user does not exist.
   */
  public static String getUsernameFromUserId(long userId) {
    String sql = "SELECT username FROM users WHERE user_id = ?";
    try (Connection conn = connect();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setLong(1, userId);
      ResultSet rs = pstmt.executeQuery();

      if (rs.next()) {
        return rs.getString("username");
      }
    } catch (SQLException e) {
      LOGGER.error("Error retrieving username for user with ID={} from the database: {}", userId, e.getMessage());
    }
    return null;
  }
  /**
   * Updates the RSA public key associated with the user with the specified username in the 'users' table.
   * 
   * @param username The username of the user to update the RSA public key for.
   * @param rsaKey The new RSA public key to be associated with the user.
   * @return true if the RSA public key is successfully updated, false if the user does not exist, or if the RSA public key already exists.
   */
  public static boolean updateKey(String username, String rsaKey) {
    String selectSql = "SELECT rsa_public_key FROM users WHERE username = ?";
    String updateSql = "UPDATE users SET rsa_public_key = ? WHERE username = ?";

    try (Connection conn = connect();
         PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {

        selectStmt.setString(1, username);
        ResultSet rs = selectStmt.executeQuery();

        if (rs.next()) {
            String existingKey = rs.getString("rsa_public_key");
            if (existingKey != null) {
                // Klucz już istnieje — nie aktualizujemy
                return false;
            }
        } else {
            // Nie znaleziono użytkownika
            return false;
        }

        // Klucz nie istnieje — aktualizujemy
        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
            updateStmt.setString(1, rsaKey);
            updateStmt.setString(2, username);
            updateStmt.executeUpdate();
            return true;
        }

    } catch (SQLException e) {
        LOGGER.error("Error updating RSA key: {}", e.getMessage(), e);
        return false;
    }
  }
  /**
   * Retrieves the RSA public key associated with the user with the specified ID from the 'users' table.
   * 
   * @param userId The ID of the user to retrieve the RSA public key for.
   * @return The RSA public key associated with the user, or null if the user does not exist or if the RSA public key is not set.
   */
  public static String getPublicKey(long userId) {
    String sql = "SELECT rsa_public_key FROM users WHERE user_id = ?";

    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setLong(1, userId);
        ResultSet rs = pstmt.executeQuery();

        if (rs.next()) {
            return rs.getString("rsa_public_key"); // Może być null, jeśli nie ustawiono
        } else {
            return null; // Użytkownik nie istnieje
        }

    } catch (SQLException e) {
        LOGGER.error("Error retrieving public key for user with ID={} from the database: {}", userId, e.getMessage(), e);
        return null;
    }
}
  /**
   * Retrieves the password associated with the user with the specified username from the 'users' table.
   * 
   * @param username The username of the user to retrieve the password for.
   * @return The password associated with the user, or null if the user does not exist.
   */
  public static String getUserPassword(String username) {
    String sql = "SELECT password FROM users WHERE username = ?";
    String password = null;

    try (Connection conn = connect();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, username);
      ResultSet rs = pstmt.executeQuery();

      if (rs.next()) {
        password = rs.getString("password");
      }
    } catch (SQLException e) {
      LOGGER.error("Error retrieving password for user with username='{}' from the database: {}", username, e.getMessage(), e);
    }
    return password;
  }
  /**
   * Retrieves the user ID for the specified username.
   *
   * @param username the username for which to retrieve the user ID
   * @return the user ID if the username exists in the database, or -1 if not found
   */
  public static long getUserId(String username){
    String sql="SELECT user_id from users WHERE username = ?";
    try(PreparedStatement ps=connect().prepareStatement(sql)){
      ps.setString(1, username);
      try(ResultSet rs=ps.executeQuery()){
        if(rs.next())
          return rs.getLong("user_id");
      }
    }catch(SQLException e){
      LOGGER.error("Error checking user '{}' from the database: {}", username, e.getMessage());
    }
    return -1;
  }

/**
 * Drops a table from the database if it exists.
 *
 * @param tableName the name of the table to be dropped
 */
  public static void dropTable(String tableName) {
    // DROP TABLE statement with IF EXISTS clause to avoid an error if the table does not exist.
    String sql = "DROP TABLE IF EXISTS " + tableName;

    try (Connection conn = connect();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
      LOGGER.info("Table '{}' dropped (if it existed).", tableName);
    } catch (SQLException e) {
      LOGGER.error("Error dropping table '{}': {}", tableName, e.getMessage());
    }
  }
}