package org.project.securechat.client;
import org.project.securechat.client.sql.*;
import java.util.List;
import java.util.Arrays;

import org.junit.jupiter.api.*;
/**
 * Unit test for simple App.
 */

public class SqlTestClient
{
 
  @Test
  void serverOn(){
    //erver.main(null);
   SqlHandlerConversations.createConversationsTable();
    SqlHandlerRsa.createRsaTable();
    
  }
  @Test
  void check_database(){
   SQLiteTableViewer.main(null);
 //  SQLiteTableViewer.dropTable("conversations");
 //  SQLiteTableViewer.dropTable("rsa_public_keys");
  }
  @Test 
  void check_sorting(){
    String login1 = "sfdfaapaw";
    String login2 = "sfdfashh";
    String[] chatID = {login1,login2};
    Arrays.sort(chatID);
    String chatId = String.join(":",chatID);
    System.out.println(chatId);
  }
  @Test
  void get_usr_password(){
    //System.out.println(SqlHandlerPasswords.getUserPassword("admin"));
  }
}
