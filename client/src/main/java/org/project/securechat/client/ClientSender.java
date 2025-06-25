package org.project.securechat.client;

import java.lang.Runnable;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataOutputStream;
import java.io.IOException;

import java.util.concurrent.ExecutorService;

/**
 * Handles the sending of messages to the server.
 */
public class ClientSender implements Runnable {
  private DataOutputStream out;
  private BlockingQueue<String> clientOutputQueue;
  private static final Logger LOGGER = LogManager.getLogger();
  private ExecutorService executor;

  public ClientSender(DataOutputStream out, BlockingQueue<String> outputQueue, ExecutorService executor) {
    this.out = out;
    this.clientOutputQueue = outputQueue;
    this.executor = executor;
  }
  /**
   * Sends a message to the server
   * @param message message
   */
  public void send(String message) {
    try {
      clientOutputQueue.put(message);
    } catch (InterruptedException e) {
      LOGGER.error(e);
      executor.shutdownNow();
    }
  }
  /**
   * The main loop of ClientSender. It waits for messages in the client output queue and sends them to the server.
   */
  @Override
  public void run() {
    LOGGER.info("ClientSender working");
    try {
      while (!Thread.currentThread().isInterrupted()) {
        String message = clientOutputQueue.take();

        out.writeUTF(message);
         out.flush();
      }
    } catch (InterruptedException | IOException e) {
      LOGGER.error(e);

      executor.shutdownNow();
      try {
        // executor.shutdownNow();
        out.close();
        Thread.currentThread().interrupt();
      } catch (IOException d) {
        executor.shutdownNow();
        LOGGER.error("CLOSING STREAM ERR", d);
      }

    }

  }
}