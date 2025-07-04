package org.project.securechat.client;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;

import java.lang.Runnable;
import java.security.PublicKey;
import java.security.KeyPair;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.project.securechat.sharedClass.*;
import org.project.securechat.sharedClass.Message.DataType;
import org.project.securechat.client.sql.SqlHandlerConversations;
import org.project.securechat.client.sql.SqlHandlerMessages;
import org.project.securechat.client.sql.SqlHandlerFriends;
/**
 * Receives messages from the server and handles them according to the type of the
 * message.
 */
public class ClientReceiver implements Runnable {
  private static final Logger LOGGER = LogManager.getLogger();
  private DataInputStream in;
  private BlockingQueue<Message> serverInputQueue;
  private BlockingQueue<String> clientOutputQueue;
  private ExecutorService executor;
  

  // private HashMap<String, BlockingDeque<Message>> chatQueues = new HashMap<>();
  private final Map<DataType, Consumer<Message>> commandHandlers = new HashMap<>();
  public ClientReceiver(DataInputStream in, BlockingQueue<Message> inputQueue, BlockingQueue<String> clientOutputQueue,
     ExecutorService executor) {
    this.in = in;
    this.serverInputQueue = inputQueue;
    this.clientOutputQueue = clientOutputQueue;
    this.executor = executor;
    initCommandHandlers();
  }

  /**
   * Main loop of the client receiver. Listens for messages from the server and
   * processes them according to their type.
   */
  public void run() {
    String message=null;
    try {

      LOGGER.info("ClientReceiver working");
      
      while (!Thread.currentThread().isInterrupted()) {
        message = in.readUTF();
        System.out.println(message);
        if(message.equals("RSA_EXCHANGE")){
          LOGGER.debug("WYSYLANIE KLUCZA");
          PublicKey keyForExchange = EncryptionService.readPublicKeyFromFile();
          if(keyForExchange == null){
            KeyPair keyPair = EncryptionService.generatePairOfRsaKeys();
            EncryptionService.saveRsaKeysToFile(keyPair);
            keyForExchange = EncryptionService.readPublicKeyFromFile();
          }
          clientOutputQueue.put("RSA_EXCHANGE;"+EncryptionService.getString64FromBytes(keyForExchange.getEncoded()));
          message=in.readUTF();
        }
        if (message.startsWith("Welcome")) {
          String[] pack = message.split(";");
          Client.login = pack[1];
          Client.myID = Long.parseLong(pack[2]);
          LOGGER.info("Client login: {}", Client.login);
         
          Client.status.put("OK");
          LOGGER.info("status OK");  
          
          break;
        }else{
          //TODO handle not login
          //TODO handle offline app
        }
      }
      
      LOGGER.info("WCHODZE DO DRUGIEJ PETLI");
      // ------------------- phase 2 ----------------------
      while (!Thread.currentThread().isInterrupted()) {
        message = in.readUTF();
        LOGGER.trace("i get raw message: {}",message);
        Message mess = JsonConverter.parseDataToObject(message, Message.class);
        if(commandHandlers.containsKey(mess.getDataType()))
          commandHandlers.get(mess.getDataType()).accept(mess);
        else
          serverInputQueue.put(mess);
      }
    }catch(InterruptedException e){
      LOGGER.info("ClientReceiver interrupted, shutting down.");
      Thread.currentThread().interrupt();
    }
    catch (IOException e) {
      LOGGER.error("DRUGA PETLA {}",message, e);
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }finally{
      try {
        if(in!=null)
        in.close();
      } catch (IOException d) {
        LOGGER.error("ERROR CLOSING STREAM", d);
      }
      LOGGER.info("ClientReceiver closed");
    }
  }
  /**
   * Initializes the command handlers for processing incoming messages from the server.
   * 
   * 
   */
  private void initCommandHandlers() {
    /**
     * reacting to getting rsa
     * message is constructed as:
     * senderID: id of user
     * chatID: 0 (server)
     * DataType: RSA_KEY
     * Data: "username;rsakey"
     */
    commandHandlers.put(DataType.RSA_KEY, msg -> {
      String[] data = msg.getData().split(";");
      long user_id = msg.getSenderID();
      String username = data[0];
      String rsaKey = data[1];
      LOGGER.debug("I get information of rsa Key from user{},id:{}", username,user_id);
      if(SqlHandlerFriends.checkIfUserIdExists(user_id)){
        LOGGER.debug("public rsa of user already known");
        return;
      }
      if(user_id > 0){
        LOGGER.info("ODEBRALEM KLUCZ OD SERWERA DLA {}",username);
        SqlHandlerFriends.insertFriend(user_id,username, rsaKey);
      LOGGER.info("dodano klucz rsa dla {}",username);
      }else{
        LOGGER.info("PODANY UZYTKOWNIK NIE ISTNIEJE NA SERWERZE");
      }
      
      
    });
    /**
     * reacting to getting text message
     * message is constructed as:
     * senderID: sender
     * chatID: chat
     * DataType: TEXT
     * Data: encrypted message
     */
    commandHandlers.put(DataType.TEXT,msg ->{
      SecretKey currentAesKey = EncryptionService.getAesKeyFromString(SqlHandlerConversations.getaesKey(msg.getChatID()));
      String name=SqlHandlerConversations.getName(msg.getChatID());
      try {
        String decoded=EncryptionService.decryptWithAesKey(currentAesKey, msg.getData());
        if(ClientListener.headerId==msg.getChatID())
          System.out.println(""+name+": "+decoded);
        msg.setData(decoded);
        SqlHandlerMessages.insertMessage(msg);
      } catch (BadPaddingException e) {
        LOGGER.error("ERROR DECRYPTING MESSAGE FROM CHAT {}",msg.getChatID(),e);
      }
      // String data=EncryptionService.
      LOGGER.info("get message ");
    });
    }
}