package org.project.securechat.sharedClass;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.lang.Runnable;

public class Receiver implements Runnable{
  private DataInputStream in;
  private BlockingQueue<String> serverInputQueue;
  private volatile boolean running=true;

  public Receiver(DataInputStream in,BlockingQueue<String> inputQueue){
    this.in = in;
    this.serverInputQueue = inputQueue;
  }
  
  @Override
  public void run(){
    try{
      String message;
      while(running && (message = in.readUTF()) != null){
        serverInputQueue.put(message);
        System.out.println(message);
      }
    }
    catch(IOException | InterruptedException e){
      
    }
  }
}