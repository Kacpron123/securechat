package org.project.securechat.server;
import org.project.securechat.server.sql.SqlExecutor;
import org.project.securechat.server.sql.SqlHandlerConversations;
import org.project.securechat.server.sql.SqlHandlerMessages;
import org.project.securechat.server.sql.SqlHandlerPasswords;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
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
    
    BlockingQueue<String> clientInputQueue;

    public ClientHandler(Socket socket,String userID, BlockingQueue<String> preClientInputQueue, DataOutputStream out,ExecutorService executor){
        this.socket = socket;
        this.userID= userID;
        this.clientInputQueue=preClientInputQueue;
        this.out=out;
        this.executor = executor;
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
    public String getLogin(){
      return userID;
    }
    Message processMessage(String message){
      LOGGER.info("Otrzymalem ta wiadomosc {} : {}",userID,message);
      try{
         Message mess = JsonConverter.parseDataToObject(message,Message.class);
         LOGGER.info("WIADOMOSC SPARSOWANA");
         if(mess.getDataType().equals(DataType.TEXT)){
          executor.submit(() -> new SqlExecutor(mess));
         }
        else if(mess.getDataType().equals(DataType.RSA_KEY)){
          String rsaKey = SqlHandlerPasswords.getPublicKey(mess.getChatID());
          if(rsaKey==null){
            LOGGER.info("BRAK KLUCZA W BAZIE");
          }else{
            LOGGER.info("KLUCZ W BAZIE WYSYLAM");
          }
          Message messToSend = new Message(mess.getSenderID(),mess.getChatID(),DataType.RSA_KEY,rsaKey);
         
           out.writeUTF(JsonConverter.parseObjectToJson(messToSend));
           out.flush();
        }else if(mess.getDataType().equals(DataType.AES_EXCHANGE)){
          AesPair aesPair = JsonConverter.parseDataToObject(mess.getData(), AesPair.class);

          long user1=SqlHandlerPasswords.getUserId(mess.getSenderID());
          long user2=SqlHandlerPasswords.getUserId(mess.getChatID());
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
        }

                
         return null;
         //return mess;
      }
     catch(IOException  e){
      LOGGER.error(e);

     }
     return null;
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
          Message mess = processMessage(message);
          if(mess!=null){
            if(mess.getDataType().equals(DataType.CLOSE_CONNECTION)){
            executor.shutdownNow();
            LOGGER.error("watek ClientHandler przerwany");
            Thread.currentThread().interrupt();
            
            try{
              socket.close();
            }
            catch(IOException e){
              LOGGER.error(e);
            }
          }
          // if(mess.getChatID() != null && true==false){
          //    Server server = Server.getInstance();
          //    if(server.clients.get(mess.getChatID())!=null){
          //     try{
          //         server.clients.get(mess.getChatID()).sendMessage(JsonConverter.parseObjectToJson(mess));
          //     }catch(IOException e){
          //       LOGGER.error(e);
          //     }
              
          //    };
          // }
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
 