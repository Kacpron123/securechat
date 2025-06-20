package org.project.securechat.client;


import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.project.securechat.sharedClass.AesPair;
import org.project.securechat.sharedClass.JsonConverter;
import org.project.securechat.sharedClass.Message;
import org.project.securechat.sharedClass.Message.DataType;
import org.project.securechat.client.implementations.AesImp;
import org.project.securechat.client.implementations.RsaImp;
import org.project.securechat.client.sql.SqlHandlerConversations;
import org.project.securechat.client.sql.SqlHandlerRsa;

import java.util.HashMap;
import java.util.Map;
import java.lang.Runnable;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.PrivateKey;
public class ClientListener implements Runnable {
  private static final Logger LOGGER = LogManager.getLogger();
  private BlockingQueue<String> clientOutputQueue;
  private BufferedReader userInput;
  private ExecutorService executor;
 private final Map<String, Consumer<String>> commandHandlers = new HashMap<>();
 private final Rsa rsa = new RsaImp(); 
 private final Aes aes = new AesImp();
 private PublicKey pubKey;
 private PrivateKey privKey;
 private SecretKey currentAesKey = null;
 public ClientListener(BlockingQueue<String> clientOutputQueue, ExecutorService executor, BufferedReader userInput) {
    this.clientOutputQueue = clientOutputQueue;
    this.executor = executor;
    this.userInput = userInput;
    initCommandHandlers();
   
    privKey = EncryptionService.readPrivateKeyFromFile();
    pubKey = EncryptionService.readPublicKeyFromFile();
  }

  private long headerId = 0;
 private void initCommandHandlers() {
    commandHandlers.put("/exit", msg -> {
      Message mess = new Message(Client.login,null,DataType.CLOSE_CONNECTION,null);
      try{
        clientOutputQueue.put(JsonConverter.parseObjectToJson(mess));
      }catch(IOException |InterruptedException e){
        LOGGER.error(e);
      }
      
      System.out.println("Rozłączono z czatem.");
      executor.shutdownNow();
    });

    commandHandlers.put("/chat", msg -> {
      Message tosend=null;
      String username = msg.split(" ")[1];
      Long check_chat_id=SqlHandlerConversations.chat_2_Exist(username);
      if(check_chat_id>0){
        headerId=check_chat_id;
        return;
      }
      // check if user exist
      Long userId=SqlHandlerRsa.getUserId(username);
      if(userId==-1){
        LOGGER.debug("Don't have user in SQL\nasking Server");
        tosend=new Message(userId,0,DataType.RSA_KEY,"USERNAME:"+username);
        try{
          clientOutputQueue.put(JsonConverter.parseObjectToJson(tosend));
          Thread.sleep(200);
        }catch(Exception e){
          LOGGER.error(e);
        }
        userId=SqlHandlerRsa.getUserId(username);
        if(userId==-2){
          LOGGER.error("User: {} not exist",username);
          return;
        }
      }
      //user exist, sending requst of creating chat
      try{
        LOGGER.debug("creating_aes_key");
        SecretKey aesKey = EncryptionService.createAesKey();
        
        String rsaKeyHeader = SqlHandlerRsa.getRsaKey(userId);
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
      LOGGER.info("header set to: {}",headerId);
      
    });

    commandHandlers.put("/quit", msg -> {
      headerId = 0;
      currentAesKey = null;
      LOGGER.info("HEADER cleared KEY CLEANER");
    });
  }

  private void process(String message) {
        //System.out.println("DEBUG (ClientListener): JVM Default File Encoding: " + System.getProperty("file.encoding"));
    LOGGER.info("RAW MESSAGE "+message);
    
    String command = message.split(" ")[0].toLowerCase();
    Message mess;
    //String[] pack= encryptMessageRetWithKey(message);
    
    if(command.startsWith("/")){
      if (commandHandlers.containsKey(command)) {
        commandHandlers.get(command).accept(message);
        // mess = new Message(Client.login, header, DataType.TEXT, null);
      }else{
        LOGGER.info("COMMAND NOT FOUND");
        // nie ma potrzeby wysyłać tego do serwera
        return;
      }

    }else{
      if(headerId == 0 && currentAesKey ==null){
        // wysylanie na zaden chat
        LOGGER.info("HEADER N/A");
        return;
      }
      LOGGER.info("sending mess to {}",headerId);
      mess= new Message(Client.myID, headerId, DataType.TEXT, EncryptionService.encryptWithAesKey(currentAesKey,message));
      try {
          clientOutputQueue.put(JsonConverter.parseObjectToJson(mess));
      } catch (InterruptedException | IOException e) {
          LOGGER.error("Error while sending message", e);
      }
    }
    
    
  }
  /**
   * 
   * 
   * @return first key sec message in 64
   *
   */
  String[] encryptMessageRetWithKey(String message){
    SecretKey secretKey = aes.generateKey();
    String encodedAesMessage64 = aes.byteTo64String(aes.encodeMessage(secretKey, message.getBytes(StandardCharsets.UTF_8)));
    String encoded64Key = aes.byteTo64String(rsa.encodeMessage(pubKey, secretKey.getEncoded()));
    return new String[]{encoded64Key,encodedAesMessage64};
  }

  String decryptMessage(String aesKey,String enMessage){
     byte[] decodedKey = null;
    try{
     decodedKey = rsa.decodeMessage(privKey, aes.base64toBytes(aesKey));
    }
    catch(BadPaddingException e){
      LOGGER.error(e);
    }
     SecretKey decodedSecretKey = aes.getKeyFromBytes(decodedKey);

     
      String decodedMessage = null;
    try{
       decodedMessage = new String(aes.decodeMessage(decodedSecretKey,rsa.base64toBytes(enMessage)),StandardCharsets.UTF_8);

    }catch(BadPaddingException e){
      System.out.println(e);
    }
    return decodedMessage;
  }
  @Override
  public void run() {
    LOGGER.info("ClientListener working");
    String message = null;

    try {
      while (!Thread.currentThread().isInterrupted()) {
        System.out.print("Ty: ");
        message = userInput.readLine();
  
        process(message);

        // Tu można dodać wysyłanie wiadomości do serwera/sieci
        // System.out.println("Wysłano: " + message);

      }
      Thread.currentThread().interrupt();
    } catch (IOException e) {
      executor.shutdownNow();

      LOGGER.error("Błąd wejścia/wyjścia: " + e.getMessage());
    }

  }
  private String getRsaKeyFromServer(String username){
    String rsaKeyHeader = null;
    LOGGER.info("BRAK KLUCZA W BAZIE");
    Message message = new Message(Client.myID,0,DataType.RSA_KEY,username);
    LOGGER.info("WYSYLAM ZAPYTANIE O KLUCZ");
    try{
      clientOutputQueue.put(JsonConverter.parseObjectToJson(message));
      
    Thread.sleep(2000);
    }catch(InterruptedException | IOException e){
      LOGGER.error("getRsaKeyFromServer ",e);
    }
    
      LOGGER.info("SPRAWDZAM PONOWNIE CZY KLUCZ W BAZIE");
    rsaKeyHeader = SqlHandlerRsa.getRsaKey(headerId);
    LOGGER.info("KLUCZ {}",rsaKeyHeader);
    return rsaKeyHeader;
  }
}
