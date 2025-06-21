package org.project.securechat.sharedClass;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.Runnable;

public class Receiver implements Runnable {
  private DataInputStream in;
  private BlockingQueue<String> serverInputQueue;
  private volatile boolean running = true;// new LinkedBlockingDeque<>(10);
  private HashMap<String, BlockingDeque<Message>> chatQueues = new HashMap<>();
  private static final Logger LOGGER = LogManager.getLogger();
  private ExecutorService executor ;
  public Receiver(DataInputStream in, BlockingQueue<String> inputQueue,ExecutorService executor) {
    this.in = in;
    this.serverInputQueue = inputQueue;
    this.executor = executor;
  }

  @Override
  public void run() {
    LOGGER.info("RECEIVER wlaczony");
    try {
      String message;
      while (!Thread.currentThread().isInterrupted()) {
        message = in.readUTF();
        serverInputQueue.put(message);
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
      
      
      Thread.currentThread().interrupt();
    } catch (IOException | InterruptedException e) {
      LOGGER.info("watek RECEIVER przerwany");
      executor.shutdownNow();
       Thread.currentThread().interrupt();
    }
  }
}