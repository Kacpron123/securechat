package org.project.securechat.server;
import org.project.securechat.server.sql.*;


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
    SqlHandlerMessages.createMessagesTable();
    SqlHandlerConversations.createChatRelated();
  }
  @Test
  void print_Tables(){
    SQLiteTableViewer.main(null);
  }
  @Test
  void delete_database(){
    // SqlHandlerPasswords.dropTable("users");
    // SqlHandlerPasswords.dropTable("conversations");
    // SqlHandlerPasswords.dropTable("messages");
    // SqlHandlerPasswords.dropTable("chats");
    // SqlHandlerPasswords.dropTable("chat_participant");
    // SQLiteTableViewer.main(null);
  }
  @Test
  void check_database(){
   // SQLiteTableViewer.main(null);
   SQLiteTableViewer.main(null);
    //SqlHandlerPasswords.dropTable("conversations");
   // SqlHandlerMessages.createMessagesTable();
  //  SQLiteTableViewer.dropTable("conversations");
  // SQLiteTableViewer.dropTable("users");
  }
  // @Test 
  // void check_sorting(){
  //   String login1 = "sfdfaapaw";
  //   String login2 = "sfdfashh";
  //   String[] chatID = {login1,login2};
  //   Arrays.sort(chatID);
  //   String chatId = String.join(":",chatID);
  //   System.out.println(chatId);
  // }
  // @Test
  // void get_usr_password(){
  //   System.out.println(SqlHandlerPasswords.getUserPassword("admin"));
  // }

}
