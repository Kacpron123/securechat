package org.project.securechat.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.lang.Runnable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.project.securechat.sharedClass.*;;
public class ClientReceiver implements Runnable{
    private static final Logger LOGGER = LogManager.getLogger();
  private DataInputStream in;
  private BlockingQueue<String> serverInputQueue;
  private BlockingQueue<String>clientOutputQueue;
    private ExecutorService executor;
 
  private HashMap<String, BlockingDeque<Message>> chatQueues = new HashMap<>();
  public ClientReceiver(DataInputStream in,BlockingQueue<String> inputQueue,BlockingQueue<String> clientOutputQueue
  ,  ExecutorService executor){
    this.in = in;
    this.serverInputQueue = inputQueue;
    this.clientOutputQueue = clientOutputQueue;
    this.executor = executor;
  }
  
  @Override
  public void run(){
    try{
      
      LOGGER.info("ClientReceiver working");
      String message;
      while (!Thread.currentThread().isInterrupted()){
        message = in.readUTF();
          System.out.println(message);
        if(message.startsWith("Welcome")){
          Client.status.put("OK");
          LOGGER.info("status OK");
          break;
      }
    }
      while(!Thread.currentThread().isInterrupted()){
        message = in.readUTF();
          System.out.println(message);
        
        //serverInputQueue.put(message);
        //Message mess = JsonConverter.parseDataToObject(message, Message.class);
       // if(chatQueues.containsKey(mess.getChatID())){
        //  chatQueues.get(mess.getChatID()).put(mess);
       // }else{
       //   chatQueues.put(mess.getChatID(),new LinkedBlockingDeque<Message>(10));
        //}
      
      }
    }
    catch(IOException |InterruptedException e){
      LOGGER.error("",e);
      executor.shutdownNow();
       
      try{
         in.close();
      } 
      catch(IOException d){
        LOGGER.error("ERROR CLOSING STREAM",d);
      }
      Thread.currentThread().interrupt();
    }
  }
}