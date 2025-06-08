package org.project.securechat.client.sql;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
public class SqlHandlerRsa {
   private static final String DB_URL = "jdbc:sqlite:client_database.db";

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
    public static void createRsaTable() {
  String sql = "CREATE TABLE IF NOT EXISTS public_keys (" +
    "login VARCHAR(100) PRIMARY KEY," +
    "rsa_public_key TEXT" +
");";

    try (Connection conn = connect();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
      System.out.println("Tabela 'public_keys' została utworzona (jeśli nie istniała).");
    } catch (SQLException e) {
      System.err.println("Błąd podczas tworzenia tabeli: " + e.getMessage());
    }
  }
  public static String getRsaKey(String login){
    String sql = "SELECT * FROM public_keys WHERE login = ?";
    String rsaKey = null;
     try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, login);
        ResultSet rs = pstmt.executeQuery();
        
        if (rs.next()) {
            rsaKey =  rs.getString("rsa_public_key");
            
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
     return rsaKey;
}
public static boolean insertKey(String login, String rsaPublicKey) {
    String sql = "INSERT OR IGNORE INTO public_keys (login, rsa_public_key) VALUES (?, ?)";

    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, login);
        pstmt.setString(2, rsaPublicKey);

        int affectedRows = pstmt.executeUpdate();
        return affectedRows > 0; // true jeśli dodano, false jeśli już był

    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
}

  }
 
