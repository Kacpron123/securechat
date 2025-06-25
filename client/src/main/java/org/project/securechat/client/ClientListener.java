package org.project.securechat.client;


import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import javax.crypto.SecretKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.project.securechat.sharedClass.JsonConverter;
import org.project.securechat.sharedClass.Message;
import org.project.securechat.sharedClass.Message.DataType;
import org.project.securechat.client.sql.SqlHandlerConversations;
import org.project.securechat.client.sql.SqlHandlerFriends;
import org.project.securechat.client.sql.SqlHandlerMessages;

import java.util.HashMap;
import java.util.Map;
import java.lang.Runnable;
import java.security.PublicKey;
public class ClientListener implements Runnable {
  private static final Logger LOGGER = LogManager.getLogger();
  private BlockingQueue<String> clientOutputQueue;
  private BufferedReader userInput;
  private ExecutorService executor;
  private final Map<String, Consumer<String>> commandHandlers = new HashMap<>();
  private PublicKey pubKey;
  private SecretKey currentAesKey = null;
  public ClientListener(BlockingQueue<String> clientOutputQueue, ExecutorService executor, BufferedReader userInput) {
    this.clientOutputQueue = clientOutputQueue;
    this.executor = executor;
    this.userInput = userInput;
    initCommandHandlers();
    pubKey = EncryptionService.readPublicKeyFromFile();
  }

  static public long headerId = 0;

  /**
   * Variable to store the current chat header id. Used to send the message to
   * the correct chat.
   */
  private void initCommandHandlers() {

    /**
     * Command to exit from the program.
     */
    commandHandlers.put("/exit", msg -> {
      Message mess = new Message(Client.myID,0,DataType.CLOSE_CONNECTION,null);
      try{
        clientOutputQueue.put(JsonConverter.parseObjectToJson(mess));
      }catch(IOException |InterruptedException e){
        LOGGER.error(e);
      }
      System.out.println("Disconnected from chat.");
      executor.shutdownNow();
    });
    /**
     * moving to chat with user
     * if dont't have than chat is created
     * if user isn't known the rsa question is invoke
     */
    commandHandlers.put("/chat", msg -> {
      Message tosend=null;
      String username = msg.split(" ")[1];
      Long check_chat_id=SqlHandlerConversations.chat_2_Exist(username);
      if(check_chat_id>0){
        System.out.println("header set to: "+username);
        headerId=check_chat_id;
        SqlHandlerMessages.loadMessages(headerId,8);
        currentAesKey = EncryptionService.getAesKeyFromString(SqlHandlerConversations.getaesKey(headerId));
        return;
      }
      // check if user exist
      Long userId=SqlHandlerFriends.getUserId(username);
      if(userId==-1){
        LOGGER.debug("Don't have user in SQL\nasking Server");
        tosend=new Message(Client.myID,0,DataType.RSA_KEY,"USERNAME:"+username);
        try{
          clientOutputQueue.put(JsonConverter.parseObjectToJson(tosend));
          Thread.sleep(200);
        }catch(Exception e){
          LOGGER.error(e);
        }
        userId=SqlHandlerFriends.getUserId(username);
        if(userId==-2){
          System.out.println("user not exist");
          LOGGER.error("User: {} not exist",username);
          return;
        }
      }
      //user exist, sending requst of creating chat
      try{
        LOGGER.debug("creating_aes_key");
        SecretKey aesKey = EncryptionService.createAesKey();

        String rsaKeyHeader = SqlHandlerFriends.getRsaKey(userId);
        PublicKey userRSA = EncryptionService.getPublicKeyFromBytes(EncryptionService.getBytesFromString64((rsaKeyHeader)));
            
        String aesMy = EncryptionService.encodeWithRsa(pubKey, aesKey.getEncoded());
        String aesUser = EncryptionService.encodeWithRsa(userRSA,aesKey.getEncoded());
        String data = ""+Client.myID+";"+aesMy+";"+
          userId+";"+aesUser;
        LOGGER.debug("sending command cerate chat");
        tosend=new Message(Client.myID,0,DataType.CREATE_2_CHAT,data);
        clientOutputQueue.put(JsonConverter.parseObjectToJson(tosend));
        Thread.sleep(200);
      }catch(Exception e){
        LOGGER.error(e);
      }
      check_chat_id=SqlHandlerConversations.chat_2_Exist(username);
      if(check_chat_id==-1){
        LOGGER.error("error in creating chat");
        return;
      }
      headerId=check_chat_id;
      currentAesKey = EncryptionService.getAesKeyFromString(SqlHandlerConversations.getaesKey(headerId));
      System.out.println("header set to: "+username);
    });

    /**
     * Clearing header and aes key
     */
    commandHandlers.put("/quit", msg -> {
      headerId = 0;
      currentAesKey = null;
      System.out.println("header cleared");
    });
    /**
     * Question of the list of commands that the client recognizes
     */
    commandHandlers.put("/help", msg -> {
      System.out.println("/exit\texit an app");
      System.out.println("/quit\texit from current chat");
      System.out.println("/chat [name]\t open chat [name]");
    });
  }

  /**
   * Processes given message from user. It looks for command in message and
   * handles it if it knows the command. If not, it sends the message to server.
   * @param message raw message from user
   */
  private void process(String message) {
    LOGGER.trace("RAW MESSAGE "+message);
    
    String command = message.split(" ")[0].toLowerCase();
    Message mess;
    
    if(command.startsWith("/")){
      if (commandHandlers.containsKey(command)) {
        commandHandlers.get(command).accept(message);
      }
      else{
        LOGGER.info("COMMAND NOT FOUND");
        return;
      }

    }
    else{
      if(headerId == 0){
        System.out.println("You are not in any chat, use [/help] for help");
        return;
      }
      mess= new Message(Client.myID, headerId, DataType.TEXT, EncryptionService.encryptWithAesKey(currentAesKey,message));
      try {
          clientOutputQueue.put(JsonConverter.parseObjectToJson(mess));
          LOGGER.info("sending message to {}", headerId);
          return;
      } catch (InterruptedException | IOException e) {
          LOGGER.error("Error while sending message", e);
      }
    }
    
    
  }
  /**
   * The main loop of ClientListener. It waits for user input and processes it.
   */
  @Override
  public void run() {
    LOGGER.info("ClientListener working");
    String message = null;

    try {
      while (!Thread.currentThread().isInterrupted()) {
        System.out.print("Ty: ");
        
        message = userInput.readLine();
        process(message);

      }
      Thread.currentThread().interrupt();
    } catch (IOException e) {
      executor.shutdownNow();
      LOGGER.error("Input/output error: " + e.getMessage());
    }

  }
}
