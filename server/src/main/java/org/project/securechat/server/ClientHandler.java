package org.project.securechat.server;
import org.project.securechat.server.sql.SqlHandlerConversations;
import org.project.securechat.server.sql.SqlHandlerMessages;
import org.project.securechat.server.sql.SqlHandlerPasswords;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.lang.Runnable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.project.securechat.sharedClass.Message.DataType;
import org.project.securechat.sharedClass.*;

/**
 * Handles communication with a single client.
 * 
 * It receives messages from the client by Receiver and handle them according to the type of the
 * message.
 */
public class ClientHandler implements Runnable{
    private static final Logger LOGGER = LogManager.getLogger(); 
    public Socket socket;
    private ExecutorService executor ;
    DataOutputStream out;
    public long userID;
    private final Map<Message.DataType,Function<Message,Message>> commandHandler= new HashMap<>();
    BlockingQueue<String> clientInputQueue;

    public ClientHandler(Socket socket,Long userID, BlockingQueue<String> preClientInputQueue, DataOutputStream out,ExecutorService executor){
        this.socket = socket;
        this.clientInputQueue=preClientInputQueue;
        this.out=out;
        this.executor = executor;
        this.userID=userID;
        initCommandHandlers();
      
    }
    /**
     * Initialize command handlers for processing incoming messages from the client.
     * 
     * It reacts to different types of messages and performs appropriate actions.
     */
    private void initCommandHandlers(){
      /**
       * stoping the connection
       */
      commandHandler.put(DataType.CLOSE_CONNECTION,msg->{
        executor.shutdownNow();
        LOGGER.info("watek ClientHandler przerwany");
        Thread.currentThread().interrupt();
        
        try{
          socket.close();
        }
        catch(IOException e){
          LOGGER.error(e);
        }
        return null;
      });
      /**
       * reacting to getting question about rsa
       * message is constructed as:
       * senderID: id of user who asked
       * chatID: 0 (server)
       * DataType: RSA_KEY
       * Data: "(ID/USERNAME): data"
       */
      commandHandler.put(DataType.RSA_KEY,msg->{
        String username=msg.getData();
        LOGGER.debug("i get question about public_rsa of user: {}:",username);
        long user_id;
        String fragment = username.split(":")[1];
        LOGGER.trace("checking rsa for: {}",fragment);
        if(username.startsWith("USERNAME:"))
          user_id=SqlHandlerPasswords.getUserId(fragment);
        else
          user_id=Long.parseLong(fragment);
        username=SqlHandlerPasswords.getUsernameFromUserId(user_id);

        String rsaKey = SqlHandlerPasswords.getPublicKey(user_id);
        LOGGER.debug((rsaKey == null) ? "BRAK KLUCZA W BAZIE" : "KLUCZ W BAZIE WYSYLAM");
        try{
          Message tosend=new Message(user_id,0,DataType.RSA_KEY,username+';'+rsaKey);
          out.writeUTF(JsonConverter.parseObjectToJson(tosend));
          out.flush();
        }catch(IOException e){
          LOGGER.error(e);
        }
         return null;
      });
      /**
       * reacting to getting request of creating chat
       * message is constructed as:
       * senderID: id of creator
       * chatID: 0 (server)
       * DataType: CREATE_2_CHAT
       * Data: "id1;aes1;id2;aes2"
       */
      commandHandler.put(DataType.CREATE_2_CHAT,msg->{
          LOGGER.debug("creating new chat");
          // long creatorID = msg.getSenderID();
          String[] data = msg.getData().split(";");
          long user1Id = Long.parseLong(data[0]);
          String aes1 = data[1];
          long user2Id = Long.parseLong(data[2]);
          String aes2 = data[3];
          try {
              // TODO creator create chat, don't make message "crossing"
              long chatId = SqlHandlerConversations.insertOneToOneChat(user1Id, user2Id, aes1, aes2);
              LOGGER.debug("sending messages about  creating chat to members:");
              Message confirmationMessage = new Message(user2Id, chatId, DataType.CREATE_2_CHAT, aes1);
              out.writeUTF(JsonConverter.parseObjectToJson(confirmationMessage));
              out.flush();
              confirmationMessage = new Message(user1Id, chatId, DataType.CREATE_2_CHAT, aes2);
              Server.sendMessage(user2Id,confirmationMessage);
              
          } catch (SQLException e) {
              LOGGER.error("Error creating one-to-one chat: {}", e.getMessage());
          } catch (IOException e) {
              LOGGER.error("Error sending confirmation message: {}", e.getMessage());
          }

          return null;

          // long chatId=SqlHandlerConversations.insertOneToOneChat(creatorID, secondUserId, aesKey, aesKey);
          // SqlHandlerMessages.insertMessage(creatorID, secondUserId, DataType.CREATE_2_CHAT, aesKey);
          
        });
      /**
       * reacting to getting text message
       * message is constructed as:
       * senderID: id of sender
       * chatID: chat
       * DataType: TEXT
       * Data: "encrypted message"
       */
      commandHandler.put(DataType.TEXT,msg->{
      try {
          long chatId = msg.getChatID();
          long senderId = msg.getSenderID();
          List<Long> usersInChat = SqlHandlerConversations.getUsersFromChat(chatId, senderId);
          for (Long userId : usersInChat) {
              Message mess=new Message(userId,msg.getChatID(),DataType.TEXT,msg.getData());
              Server.sendMessage(userId, mess);
          }
      } catch (Exception e) {
          LOGGER.error("Invalid chat ID or sender ID format: {}", e.getMessage());
      }

        return null;
      });    
    }
    /**
     * Get the ID of the user associated with this client.
     * 
     * @return ID of the user.
     */
    public Long getID(){
      return userID;
    }
    /**
     * Sends a message to the client.
     *
     * @param mess The message object to be sent.
     */
    public void sendMessage(Message mess){
      try {
        out.writeUTF(JsonConverter.parseObjectToJson(mess));
      } catch (IOException e) {
        LOGGER.error("Error while sending message", e);
      }
    }
    /**
     * Sends a message to the client.
     *
     * @param message The message in string format to be sent.
     */
    public void sendMessage(String message){
      try{  
         out.writeUTF(message);
      } catch(IOException e){
        LOGGER.error(e);
        Message mess = null;
        try{
        mess = JsonConverter.parseDataToObject(message,Message.class);
        }
      catch(IOException d){
        LOGGER.error(d);
      }
      LOGGER.debug("klienta {} nie ma na serwerze",mess.getChatID());
    //  server.removeClient(mess.getChatID()); whyy?
    }
     

    }
    /**
     * Main loop of the client handler. It receives messages from the client and
     * handles them according to their type.
     */
    @Override
    public void run(){
      try{
        
        LOGGER.info("Starting ClientHandler: {}",userID);
        // sending old messages
        List<Message> olderMessages = SqlHandlerMessages.getOlderMessages(userID);
        for(Message m:olderMessages){
          System.out.println(m.toString());
        }
        if(!olderMessages.isEmpty()){
          LOGGER.info("sending old messages to {}",userID);
          for(Message m:olderMessages){
            try{
              String jsonMessage = JsonConverter.parseObjectToJson(m);
              sendMessage(jsonMessage);
              if(!m.getDataType().equals(DataType.TEXT))
                Thread.sleep(200);
            }catch(IOException e){
              // LOGGER.error("error in sending old message to {}",userID);
            }
          }
        }
        String message=null;
        while(!Thread.currentThread().isInterrupted()){
          message=clientInputQueue.take();
          try{
            Message mess = JsonConverter.parseDataToObject(message,Message.class);
            commandHandler.get(mess.getDataType()).apply(mess);
          }catch(IOException e){
            LOGGER.info(e);
            }
          
          }
         
        }
        catch(InterruptedException  e){
          System.out.println(e);
        }finally{
          try{
            
            LOGGER.info("ClientHandler for {} closing socket.",userID);
            socket.close();
            Server.getInstance().removeClient(userID);
          }catch(IOException e){
            e.printStackTrace();
          }
          boolean updateLastTime=SqlHandlerPasswords.updateLastLoginTime(userID,Instant.now());
          if(updateLastTime){
            LOGGER.info("Zaktualizowano czas ostatniej logowania uzytkownika {}",userID);
          }else{
            LOGGER.info("Bład podczas aktualizacji czasu ostatniej logowania uzytkownika {}",userID);
          }
        }
    }
}
 