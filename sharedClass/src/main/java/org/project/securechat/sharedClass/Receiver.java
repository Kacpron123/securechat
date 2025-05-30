package org.project.securechat.sharedClass;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.lang.Runnable;

public class Receiver implements Runnable{
  private DataInputStream in;
  private BlockingQueue<String> serverInputQueue;
  private volatile boolean running=true;//new LinkedBlockingDeque<>(10);
  private HashMap<String, BlockingDeque<Message>> chatQueues = new HashMap<>();
  public Receiver(DataInputStream in,BlockingQueue<String> inputQueue){
    this.in = in;
    this.serverInputQueue = inputQueue;
  }
  
  @Override
  public void run(){
    try{
      String message;
      while(running ){
        message = in.readUTF();
        serverInputQueue.put(message);
        /*
        Message mess = JsonConverter.parseDataToObject(message, Message.class);
        if(chatQueues.containsKey(mess.getChatID())){
          chatQueues.get(mess.getChatID()).put(mess);
        }else{
          chatQueues.put(mess.getChatID(),new LinkedBlockingDeque<Message>(10));
        }
          
        System.out.println(message);
         */
      }
    }
    catch(IOException | InterruptedException e){
      
    }
  }
}