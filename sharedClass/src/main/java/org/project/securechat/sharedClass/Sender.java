package org.project.securechat.sharedClass;

import java.lang.Runnable;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;
import java.io.DataOutputStream;
import java.io.IOException;

public class Sender implements Runnable {
  private DataOutputStream out;
  private BlockingQueue<String> clientOutputQueue;
  Function<String, String> procesing;

  public Sender(DataOutputStream out, BlockingQueue<String> outputQueue, Function<String, String> procesing) {
    this.out = out;
    this.clientOutputQueue = outputQueue;
    this.procesing = (s) -> s;
  }

  @Override
  public void run() {
    try {
      while (true) {
        String message = clientOutputQueue.take();
        String processedMessage = procesing.apply(message);
        out.writeUTF(processedMessage);
      }
    } catch (InterruptedException | IOException e) {
      System.out.println(e);
    }
  }
}