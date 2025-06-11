package org.project.securechat.client;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.db.jpa.converter.MessageAttributeConverter;
import org.project.securechat.sharedClass.AesPair;
import org.project.securechat.sharedClass.JsonConverter;
import org.project.securechat.sharedClass.Message;
import org.project.securechat.sharedClass.Message.DataType;
import org.project.securechat.client.implementations.AesImp;
import org.project.securechat.client.implementations.RsaImp;
import org.project.securechat.client.sql.SqlHandlerConversations;
import org.project.securechat.client.sql.SqlHandlerRsa;

import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.lang.Runnable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.KeyPair;
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

  private String header = "N/A";
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
      String[] data = msg.split(" ");
      if (data.length > 1) {

        header = data[1];
        
        LOGGER.info("SPRAWDZ CZY KLUCZ W BAZIE {}",header);
        String rsaKeyHeader = SqlHandlerRsa.getRsaKey(header);
        LOGGER.info("KLUCZ W BAZIE {}",rsaKeyHeader);
        try{
          if(rsaKeyHeader == null){
            LOGGER.info("BRAK KLUCZA W BAZIE");
          Message message = new Message(Client.login,header,DataType.GET_RSA_KEY,null);
          LOGGER.info("WYSYLAM ZAPYTANIE O KLUCZ");
          clientOutputQueue.put(JsonConverter.parseObjectToJson(message));
            
          Thread.sleep(2000);
           LOGGER.info("SPRAWDZAM PONOWNIE CZY KLUCZ W BAZIE");
          rsaKeyHeader = SqlHandlerRsa.getRsaKey(header);
          LOGGER.info("KLUCZ {}",rsaKeyHeader);
        }
        }catch(InterruptedException |IOException e){
          LOGGER.error(e);
        }
        SecretKey aesKey = null;
        Map<String,String> dataConversation = SqlHandlerConversations.getConversation(Client.login,header);
        if(dataConversation == null){
          LOGGER.info("KONWERSACJI NIE MA BAZIE");
          aesKey = EncryptionService.createAesKey();
          LOGGER.info("TWORZE KLUCZE AES");
          try{
            PublicKey headerKey = EncryptionService.getPublicKeyFromBytes(EncryptionService.getBytesFromString64((rsaKeyHeader)));
            
            String aesKeyHeader = EncryptionService.encodeWithRsa(headerKey,aesKey.getEncoded());
            String aesKeyClient = EncryptionService.encodeWithRsa(pubKey, aesKey.getEncoded());
            LOGGER.info("STWORZYLEM KLUCZE DLA {} {}",Client.login,header);
            AesPair aesPair = new AesPair( aesKeyClient,aesKeyHeader,Client.login, header);
            LOGGER.info("STWORZYLEM PARE KLUCZY AES {}",JsonConverter.parseObjectToJson(aesPair));
            Message message = new Message(Client.login,header,DataType.AES_EXCHANGE,JsonConverter.parseObjectToJson(aesPair));
            clientOutputQueue.put(JsonConverter.parseObjectToJson(message));
            Thread.sleep(2000);
            dataConversation = SqlHandlerConversations.getConversation(Client.login,header);
            
          }catch(Exception e){
            LOGGER.error("BLAD KONWERSJI RSA PUB FROM BYTES",e);
          }
            
        }
        LOGGER.info("OTO dataConversation {} \n {} ",dataConversation.get("user1"),dataConversation.get("aes_key_for_user1"));
        String aesKeyStr = null;
        if(dataConversation.get("user1").equals(Client.login)){
          aesKeyStr = dataConversation.get("aes_key_for_user1");
          
        }else{
           aesKeyStr = dataConversation.get("aes_key_for_user2");
        }
        LOGGER.info("KLUCZ STR {}",aesKeyStr);
        try{
          currentAesKey = EncryptionService.getAesFromBytes(EncryptionService.decodeWithRsa(privKey,aesKeyStr));
          LOGGER.info("DESZYFRACJA UDANA");
          if (currentAesKey == null) {
              LOGGER.error("currentAesKey jest null!");
          } else {
          LOGGER.info("currentAesKey OK, długość klucza: " + currentAesKey.getEncoded().length);
        }
         
        LOGGER.info("HEADER {}", header);
        
      }
        catch(Throwable e){
          LOGGER.error("DEKRYPCJA ZLYM KLUCZEM ",e);
        }
      }
    }
    );

    commandHandlers.put("/quit", msg -> {
      header = "N/A";
      currentAesKey = null;
      LOGGER.info("HEADER cleared KEY CLEANER");
    });
  }

  private void process(String message) {
        //System.out.println("DEBUG (ClientListener): JVM Default File Encoding: " + System.getProperty("file.encoding"));
    LOGGER.info("RAW MESSAGE "+message);
    
    String command = message.split(" ")[0].toLowerCase();
    Message mess  = new Message(Client.login,null,DataType.TEXT,null);;
    //String[] pack= encryptMessageRetWithKey(message);
    
    if(command.startsWith("/")){
      if (commandHandlers.containsKey(command)) {
        commandHandlers.get(command).accept(message);
        mess = new Message(Client.login, header, DataType.TEXT, null);
      }else{
        LOGGER.info("COMMAND NOT FOUND");
        // nie ma potrzeby wysyłać tego do serwera
        return;
      }

    }else{
      if(header == "N/A" && currentAesKey ==null){
        // wysylanie na zaden chat
        LOGGER.info("HEADER N/A");
        return;
      }
      LOGGER.info("sending mess to {}",header);
      mess= new Message(Client.login, header, DataType.TEXT, EncryptionService.encryptWithAesKey(currentAesKey,message));
    }
    
    //LOGGER.info("Odszyfrowany mess {}", decryptMessage(pack[0], pack[1]));
    
    
    
    try {
      String jMess = JsonConverter.parseObjectToJson(mess);
      LOGGER.debug("PROCCED MESS {}", jMess);
      clientOutputQueue.put(jMess); // podwójne JsonConverter usunięte
    } catch (InterruptedException | IOException e) {
      LOGGER.error("process : ", e);
      Thread.currentThread().interrupt();
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
}
