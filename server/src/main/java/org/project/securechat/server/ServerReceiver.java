package org.project.securechat.server;


import java.io.DataInputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import org.project.securechat.server.sql.SqlHandlerMessages;
import org.project.securechat.sharedClass.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.Runnable;

/**
 * Listens for messages from the clients and adds them to the server input queue.
 * 
 * It is a part of the server and is used to receive messages from the clients.
 * It is a Runnable and can be run in a separate thread.
 */
// (TODO ClientHandler)
public class ServerReceiver implements Runnable {
  private DataInputStream in;
  private BlockingQueue<String> serverInputQueue;
  // new LinkedBlockingDeque<>(10);
  // private HashMap<String, BlockingDeque<Message>> chatQueues = new HashMap<>();
  private static final Logger LOGGER = LogManager.getLogger();
  private ExecutorService executor ;
  public ServerReceiver(DataInputStream in, BlockingQueue<String> inputQueue,ExecutorService executor) {
    this.in = in;
    this.serverInputQueue = inputQueue;
    this.executor = executor;
  }

  /**
   * Main loop of the receiver. Listens for messages from the clients and adds
   * them to the server input queue.
   */
  @Override
  public void run() {
    LOGGER.info("RECEIVER wlaczony");
    try {
      String message;
      Message mess;
      while (!Thread.currentThread().isInterrupted()) {
        message = in.readUTF();
        LOGGER.trace("I get raw message:\n{}",message);
        // TODO create new thread to save messages
        mess = JsonConverter.parseDataToObject(message, Message.class);
        SqlHandlerMessages.insertMessage(mess);
        serverInputQueue.put(message);
      }
      
      
      //Thread.currentThread().interrupt();
    } catch (IOException | InterruptedException e) {
      LOGGER.info("watek RECEIVER przerwany",e);
      executor.shutdownNow();
       Thread.currentThread().interrupt();
    }
  }
}