package org.project.securechat.client;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.project.securechat.sharedClass.JsonConverter;
import org.project.securechat.sharedClass.Message;
import org.project.securechat.sharedClass.Message.DataType;

import java.util.Scanner;
import java.lang.Runnable;
public class ClientListener implements Runnable{
    private static final Logger LOGGER = LogManager.getLogger();
  private BlockingQueue<String> clientOutputQueue;
  private BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
  private ExecutorService executor;
  public ClientListener(BlockingQueue<String> clientOutputQueue,ExecutorService executor){
    this.clientOutputQueue = clientOutputQueue;
    this.executor = executor;
  } 
  private String header = "";
  void process(String message){
    LOGGER.info("RAW MESSAGE {}",message);
    Message mess = new Message(Client.login,"N/A",DataType.TEXT,"N/A");
    LOGGER.info("OBJ MESSAGE CREATED {}",mess);
    if ("/exit".equalsIgnoreCase(message)) {
            System.out.println("Rozłączono z czatem.");
            executor.shutdownNow();
          
          }
    else if(message.startsWith("/chat")){
      String[] data = message.split(" ");
      header = data[1];
      LOGGER.info("HEADER {}",header);
      mess = new Message(Client.login,header,DataType.TEXT,"witam");
      
    }
    
    try{
      String jMess = JsonConverter.parseObjectToJson(mess);
    LOGGER.info("PROCCED MESS {}",jMess);
       clientOutputQueue.put(JsonConverter.parseObjectToJson(jMess));  
    }
    catch(InterruptedException |IOException e){
      LOGGER.error("process : ",e);
    }
       
  }
  @Override
  public void run(){
    LOGGER.info("ClientListener working");
    String message = null;
    
    
      try {
         while (!Thread.currentThread().isInterrupted()) {
        System.out.print("Ty: ");
        message = userInput.readLine();

        process(message);
          
         // Tu można dodać wysyłanie wiadomości do serwera/sieci
          //System.out.println("Wysłano: " + message);

      }
     Thread.currentThread().interrupt();
    } catch (IOException  e) {
        executor.shutdownNow();
        
      LOGGER.error("Błąd wejścia/wyjścia: " + e.getMessage());
     }
  


  }
}
