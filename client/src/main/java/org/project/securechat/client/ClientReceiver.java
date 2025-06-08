package org.project.securechat.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.lang.Runnable;
import java.security.PublicKey;
import java.sql.DataTruncation;
import java.security.KeyPair;
import java.security.PrivateKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.project.securechat.sharedClass.*;
import org.project.securechat.sharedClass.Message.DataType;
import org.project.securechat.client.implementations.*;
import org.project.securechat.client.sql.SqlHandlerRsa;
public class ClientReceiver implements Runnable {
  private static final Logger LOGGER = LogManager.getLogger();
  private DataInputStream in;
  private BlockingQueue<String> serverInputQueue;
  private BlockingQueue<String> clientOutputQueue;
  private ExecutorService executor;

  private HashMap<String, BlockingDeque<Message>> chatQueues = new HashMap<>();
 private final Map<DataType, Consumer<Message>> commandHandlers = new HashMap<>();
  public ClientReceiver(DataInputStream in, BlockingQueue<String> inputQueue, BlockingQueue<String> clientOutputQueue,
     ExecutorService executor) {
    this.in = in;
    this.serverInputQueue = inputQueue;
    this.clientOutputQueue = clientOutputQueue;
    this.executor = executor;
    initCommandHandlers();
  }
  private void exchangeRsaKey(){
    LOGGER.info("WYSYLANIE KLUCZA");
    Rsa rsa = new RsaImp();
    PublicKey keyForExchange = rsa.readPubKeyFromFile();
    if(keyForExchange == null){
      KeyPair keyPair = rsa.generatePairOfKeys();
      rsa.writeKeysToFile(keyPair);
      keyForExchange = rsa.readPubKeyFromFile();
      
    }
    LOGGER.info("KLUCZ ODCZYTANY | STWORZONY");
    Message messageKeyExchange = new Message(Client.login,null,DataType.KEY_EXCHANGE,rsa.byteTo64String(keyForExchange.getEncoded()));
    try{
        clientOutputQueue.put(JsonConverter.parseObjectToJson(messageKeyExchange));
        LOGGER.info("KLUCZ WYSLANY");
      }catch(InterruptedException |IOException e){
      LOGGER.error("BLAD WYMIANY KLUCZY",e);
    }
    }
  
  @Override
  public void run() {
    String message=null;
    try {

      LOGGER.info("ClientReceiver working");
      
      while (!Thread.currentThread().isInterrupted()) {
        message = in.readUTF();
        System.out.println(message);
        if (message.startsWith("Welcome")) {
          
         
          message = in.readUTF();
          Message mess = JsonConverter.parseDataToObject(message, Message.class);
          LOGGER.info("JESTEM W PIERWSZEJ PETLI {}",message);
          if(mess.getDataType().equals(DataType.CONFIRMATION)){
            Client.login = mess.getData();
          }else if(mess.getDataType().equals(DataType.KEY_EXCHANGE)){
              Client.login = mess.getData();
               exchangeRsaKey();
          }
           Client.status.put("OK");
          LOGGER.info("status OK");  
          
          break;
        }
      }
      LOGGER.info("WCHODZE DO DRUGIEJ PETLI");
      while (!Thread.currentThread().isInterrupted()) {
        message = in.readUTF();
        processMessage(message);
        System.out.println(message);

        // serverInputQueue.put(message);
        // Message mess = JsonConverter.parseDataToObject(message, Message.class);
        // if(chatQueues.containsKey(mess.getChatID())){
        // chatQueues.get(mess.getChatID()).put(mess);
        // }else{
        // chatQueues.put(mess.getChatID(),new LinkedBlockingDeque<Message>(10));
        // }

      }
    } catch (IOException | InterruptedException e) {
      LOGGER.error("DRUGA PETLA {}",message, e);
      executor.shutdownNow();

      try {
        in.close();
      } catch (IOException d) {
        LOGGER.error("ERROR CLOSING STREAM", d);
      }
      Thread.currentThread().interrupt();
    }
  }
  private void processMessage(String message)throws IOException {
        //System.out.println("DEBUG (ClientListener): JVM Default File Encoding: " + System.getProperty("file.encoding"));
    LOGGER.info("RAW MESSAGE "+message);
    
    
    Message mess = JsonConverter.parseDataToObject(message, Message.class);
    //String[] pack= encryptMessageRetWithKey(message);
    
    
    if (commandHandlers.containsKey(mess.getDataType())) {
      commandHandlers.get(mess.getDataType()).accept(mess);
    }
    
  }

  private void initCommandHandlers() {
    commandHandlers.put(DataType.GET_RSA_KEY, msg -> {
      LOGGER.info("ODEBRALEM KLUCZ OD SERWERA DLA {}",msg.getChatID());
      String rsaKey = msg.getData();
      String forWho = msg.getChatID();
      if(rsaKey !=null){
         SqlHandlerRsa.insertKey(forWho, rsaKey);
      LOGGER.info("dodano klucz rsa dla {}",forWho);
      }else{
        LOGGER.info("PODANY UZYTKOWNIK NIE ISTNIEJE");
      }
     
      
    });

  
    }
}