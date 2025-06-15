package org.project.securechat.server;
import org.project.securechat.server.sql.SqlExecutor;
import org.project.securechat.server.sql.SqlHandlerConversations;
import org.project.securechat.server.sql.SqlHandlerMessages;
import org.project.securechat.server.sql.SqlHandlerPasswords;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.lang.Runnable;
import java.time.LocalDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.project.securechat.sharedClass.Message.DataType;
import org.project.securechat.sharedClass.*;

public class ClientHandler implements Runnable{
  private static final Logger LOGGER = LogManager.getLogger(); 
    public Socket socket;
    private ExecutorService executor ;
    DataOutputStream out;
    public String userID;
    private final Map<Message.DataType,Function<Message,Message>> commandHandler= new HashMap<>();
    BlockingQueue<String> clientInputQueue;

    public ClientHandler(Socket socket,String userID, BlockingQueue<String> preClientInputQueue, DataOutputStream out,ExecutorService executor){
        this.socket = socket;
        this.userID= userID;
        this.clientInputQueue=preClientInputQueue;
        this.out=out;
        this.executor = executor;
        initCommandHandlers();
        List<Message> olderMessages = SqlHandlerMessages.getOlderMessages(userID,LocalDateTime.now());
        for(Message m:olderMessages){
          try{
            String jsonMessage = JsonConverter.parseObjectToJson(m);
            sendMessage(jsonMessage);
          }catch(IOException e){
            // LOGGER.error("error in sending old message to {}",userID);
          }
      }
      
    }
    private void initCommandHandlers(){
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
      commandHandler.put(DataType.RSA_KEY,msg->{
        String rsaKey = SqlHandlerPasswords.getPublicKey(msg.getChatID());
        LOGGER.info((rsaKey == null) ? "BRAK KLUCZA W BAZIE" : "KLUCZ W BAZIE WYSYLAM");
        try{
          out.writeUTF(JsonConverter.parseObjectToJson(new Message(msg.getSenderID(),msg.getChatID(),DataType.RSA_KEY,rsaKey)));
          out.flush();
        }catch(IOException e){
          LOGGER.error(e);
        }
         return null;
      });
      commandHandler.put(DataType.TEXT,msg->{
        executor.submit(() -> new SqlExecutor(msg));
        return null;
      });
      commandHandler.put(DataType.AES_EXCHANGE,msg->{
        try{
          AesPair aesPair = JsonConverter.parseDataToObject(msg.getData(), AesPair.class);
  
          long user1=SqlHandlerPasswords.getUserId(msg.getSenderID());
          long user2=SqlHandlerPasswords.getUserId(msg.getChatID());
          if(user1==-1 || user2==-1) //nie ma user
            return null;
  
          try{
            if(user1>user2)
              SqlHandlerConversations.insertOneToOneChat(user2, user1, aesPair.getAesReceiver(), aesPair.getAesSender());
            else
              SqlHandlerConversations.insertOneToOneChat(user1, user2, aesPair.getAesSender(), aesPair.getAesReceiver());
          }catch(SQLException e){
            LOGGER.error("creating new chat1-1 {}",e.getMessage());
          }
          // SqlHandlerConversations.insertConversation(mess.getSenderID(), mess.getChatID(), aesPair.getAesSender(),aesPair.getAesReceiver());
          // Map<String,String> conversation = SqlHandlerConversations.getConversation(mess.getSenderID(),mess.getChatID());
  
          //aesPair = new AesPair(conversation.get("aes_key_for_user1"),conversation.get("aes_key_for_user2"),conversation.get("user1"),conversation.get("user2"));
          Message messToSend = new Message(aesPair.getSender(),aesPair.getReceiver(),DataType.AES_EXCHANGE,JsonConverter.parseObjectToJson(aesPair));
          out.writeUTF(JsonConverter.parseObjectToJson(messToSend));
          out.flush();
        }catch(IOException e){
          LOGGER.error(e);
        }
        return null;
      });
    }
    public String getLogin(){
      return userID;
    }
    
    public void sendMessage(String message){
      try{  
         out.writeUTF(message);
      } catch(IOException e){
        LOGGER.error(e);
        Message mess = null;
         Server server = Server.getInstance();
         try{
         mess = JsonConverter.parseDataToObject(message,Message.class);
         
      }
     catch(IOException d){
      LOGGER.error(d);
        
      }
      LOGGER.debug("klienta {} nie ma na serwerze",mess.getChatID());
     server.removeClient(mess.getChatID());
    }
     

    }
    @Override
    public void run(){
      try{
        
        LOGGER.info("Starting ClientHandler: {}",userID);
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
          boolean updateLastTime=SqlHandlerPasswords.updateLastLoginTime(userID,LocalDateTime.now());
          if(updateLastTime){
            LOGGER.info("Zaktualizowano czas ostatniej logowania uzytkownika {}",userID);
          }else{
            LOGGER.info("Bład podczas aktualizacji czasu ostatniej logowania uzytkownika {}",userID);
          }
        }
    }
}
 