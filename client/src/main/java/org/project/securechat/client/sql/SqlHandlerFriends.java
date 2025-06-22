package org.project.securechat.client.sql;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * This class handles SQL operations related to RSA keys and user information.
 * It extends the BaseSQL class to utilize the database connection functionalities.
 */
public class SqlHandlerFriends extends BaseSQL {

  /**
   * Creates the 'friends' table to store user information, including RSA public keys.
   *
   * <p>
   * The 'friends' table contains the following columns:
   * <ul>
   * <li>{@code user_id} (INTEGER PRIMARY KEY): A unique numerical identifier for the user.</li>
   * <li>{@code login} (VARCHAR(50) NOT NULL UNIQUE): A unique login name for the user.</li>
   * <li>{@code rsa_public_key} (VARCHAR(2048) NOT NULL): The RSA public key associated with the user, used for secure communication.</li>
   * </ul>
   * </p>
   */
  public static void createRsaTable() {
    String sql = "CREATE TABLE IF NOT EXISTS friends (" +
      "user_id INTEGER PRIMARY KEY, "+
      "login VARCHAR(50) UNIQUE NOT NULL," +
      "rsa_public_key TEXT" +
      ");";

    try (Connection conn = connect();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
      System.out.println("Tabela 'friends' została utworzona (jeśli nie istniała).");
    } catch (SQLException e) {
      System.err.println("Błąd podczas tworzenia tabeli 'friends': " + e.getMessage());
    }
  }
  
  /**
   * Returns the RSA public key associated with the user with the given ID.
   * 
   * @param id The ID of the user.
   * @return The RSA public key as a string or null if an error occurs.
   */
  public static String getRsaKey(long id){
    String sql = "SELECT * FROM friends WHERE user_id = ?";
    String rsaKey = null;
     try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setLong(1, id);
        ResultSet rs = pstmt.executeQuery();
        
        if (rs.next()) {
            rsaKey =  rs.getString("rsa_public_key");
            
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
     return rsaKey;
  }
  /**
   * Inserts a new friend with the given ID, login and RSA public key into the database.
   * 
   * @param id The ID of the friend to be inserted.
   * @param login The login name of the friend to be inserted.
   * @param rsaPublicKey The RSA public key of the friend to be inserted.
   * @return true if the friend is successfully inserted, false otherwise.
   */
  public static boolean insertFriend(long id,String login, String rsaPublicKey) {
      String sql = "INSERT OR IGNORE INTO friends (user_id, login, rsa_public_key) VALUES (?, ?, ?)";

      try (Connection conn = connect();
          PreparedStatement pstmt = conn.prepareStatement(sql)) {

          pstmt.setLong(1, id);
          pstmt.setString(2, login);
          pstmt.setString(3, rsaPublicKey);

          int affectedRows = pstmt.executeUpdate();
          return affectedRows == 1; // true jeśli dodano, false jeśli już był

      } catch (SQLException e) {
          e.printStackTrace();
          return false;
      }
  }
  /**
   * Checks if a user with the specified ID exists in the friends table.
   * 
   * @param userID The ID of the user to check.
   * @return true if the user exists, false otherwise.
   */
  public static boolean checkIfUserIdExists(long userID) {
    String sql = "SELECT 1 FROM friends WHERE user_id = ?";

    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setLong(1, userID);
        ResultSet rs = pstmt.executeQuery();
        
        return rs.next();

    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
  }
  /**
   * Retrieves the login name for the user with the specified ID from the friends table.
   * 
   * @param userID The ID of the user to retrieve the login name for.
   * @return The login name of the user, or null if the user does not exist.
   */
  public static String getLogin(long userID){
    String sql = "SELECT login FROM friends WHERE user_id = ?";
    String login = null;
     try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setLong(1, userID);
        ResultSet rs = pstmt.executeQuery();
        
        if (rs.next()) {
            login =  rs.getString("login");
            
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
     return login;
}
  /**
   * Changes the login name associated with the user with the specified ID in the friends table.
   * 
   * @param userID The ID of the user to change the login name for.
   * @param login The new login name for the user.
   * @return true if the login name was successfully changed, false otherwise.
   */
  public static boolean changeLogin(long userID,String login){
    String sql = "UPDATE friends SET login = ? WHERE user_id = ?";
    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, login);
        pstmt.setLong(2, userID);
        int affectedRows = pstmt.executeUpdate();
        return affectedRows == 1; // true jeśli dodano, false jeśli już był

    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
  }
  /**
   * Retrieves the user ID for the specified login name from the friends table.
   * 
   * @param login The login name for which to retrieve the user ID.
   * @return The user ID if the login exists in the database, or -1 if not found.
   */
  public static long getUserId(String login){
    String sql = "SELECT user_id FROM friends WHERE login = ?";
    long userId = -1;
     try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, login);
        ResultSet rs = pstmt.executeQuery();
        
        if (rs.next()) {
            userId =  rs.getLong("user_id");
            
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
    return userId;
  }
}
 
