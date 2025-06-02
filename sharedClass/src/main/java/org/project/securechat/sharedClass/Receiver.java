package org.project.securechat.sharedClass;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.lang.Runnable;

public class Receiver implements Runnable{
  private DataInputStream in;
  private BlockingQueue<Message> outputQueue;
  public AtomicBoolean running=new AtomicBoolean(true);
  private Function<Message,Message> processing;
  private ShutdownSignal shutdownSignal;
  public Receiver(DataInputStream in,BlockingQueue<Message> outputQueue,Function<Message,Message> processing,ShutdownSignal shutdownSignal){
    this.in = in;
    this.outputQueue = outputQueue;
    this.processing=processing;
    this.shutdownSignal=shutdownSignal;
  }
  public void stopRunning(){
    this.running.set(false);; // Signal ClientHandler's main loop to stop
  }
  @Override
  public void run(){
    try{
        while(running.get()){
        String jsoString=in.readUTF();
        Message message = JsonConverter.parseDataToObject(jsoString,Message.class);
        Message processedMessage=processing.apply(message);      
        if(processedMessage!=null)
          outputQueue.put(processedMessage);
      }
    }
    catch(InterruptedException e){
      System.out.println("Receiver interrupted [Shutting down]: "+e.getMessage());
    }catch(IOException e){
      System.out.println("Error receiving message: "+e.getMessage());
    }finally{
      shutdownSignal.cleanup();
      this.running.set(false);
    }
    System.out.println("Receiver finished");
  }
}