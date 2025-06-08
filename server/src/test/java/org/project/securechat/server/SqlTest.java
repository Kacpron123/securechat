package org.project.securechat.server;
import org.project.securechat.server.sql.*;
import java.util.List;
import java.util.Arrays;

import org.junit.jupiter.api.*;
/**
 * Unit test for simple App.
 */

public class SqlTest
{
 
  @Test
  void serverOn(){
    //erver.main(null);
    SqlHandlerPasswords.createUsersTable();
    SqlHandlerConversations.createConversationsTable();
    SqlHandlerMessages.createMessagesTable();
    //SqlHandlerPasswords.dropTable("users");
  }
  @Test
  void check_database(){
    SQLiteTableViewer.main(null);
    //SqlHandlerMessages.createMessagesTable();
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
    System.out.println(SqlHandlerPasswords.getUserPassword("admin"));
  }
  @Test
  void tmp(){
    SqlHandlerConversations.createConversationsTable();
  }
  @Test
  void addMessage(){
    SqlHandlerMessages.insertMessage("Pawel", "Krzysztof", "haloooo", "2025-05-26T08:45:48.641588900Z");
  }

}
