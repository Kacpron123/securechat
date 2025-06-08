package org.project.securechat.server;


import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import org.project.securechat.server.sql.SqlExecutor;
import org.project.securechat.sharedClass.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.Runnable;

public class ServerReceiver implements Runnable {
  private DataInputStream in;
  private BlockingQueue<String> serverInputQueue;
  // new LinkedBlockingDeque<>(10);
  private HashMap<String, BlockingDeque<Message>> chatQueues = new HashMap<>();
  private static final Logger LOGGER = LogManager.getLogger();
  private ExecutorService executor ;
  public ServerReceiver(DataInputStream in, BlockingQueue<String> inputQueue,ExecutorService executor) {
    this.in = in;
    this.serverInputQueue = inputQueue;
    this.executor = executor;
  }

  @Override
  public void run() {
    LOGGER.info("RECEIVER wlaczony");
    try {
      String message;
      Message mess;
      while (!Thread.currentThread().isInterrupted()) {
        message = in.readUTF();
       // mess = JsonConverter.parseDataToObject(message, Message.class);
        
        serverInputQueue.put(message);
        /* 
       try{
           executor.submit(new SqlExecutor(JsonConverter.parseDataToObject(message,Message.class)));
        }catch(IOException e){
          LOGGER.info("NIE DA SIE SPARSOWWAC");
        }
        */
        /*
         * Message mess = JsonConverter.parseDataToObject(message, Message.class);
         * if(chatQueues.containsKey(mess.getChatID())){
         * chatQueues.get(mess.getChatID()).put(mess);
         * }else{
         * chatQueues.put(mess.getChatID(),new LinkedBlockingDeque<Message>(10));
         * }
         * 
         * System.out.println(message);
         */
      }
      
      
      //Thread.currentThread().interrupt();
    } catch (IOException | InterruptedException e) {
      LOGGER.info("watek RECEIVER przerwany",e);
      executor.shutdownNow();
       Thread.currentThread().interrupt();
    }
  }
}