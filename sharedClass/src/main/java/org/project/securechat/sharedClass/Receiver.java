package org.project.securechat.sharedClass;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.lang.Runnable;

public class Receiver implements Runnable{
  private BufferedReader in;
  private BlockingQueue<String> queue;
  private volatile boolean running=true;

  public Receiver(BufferedReader in,BlockingQueue<String> queue){
    this.in = in;
    this.queue = queue;
  }
  
  @Override
  public void run(){
    try{
      String message;
      while(running && (message = in.readLine()) != null){
        queue.put(message);
        System.out.println(message);
      }
    }
    catch(IOException | InterruptedException e){
      
    }
  }
}