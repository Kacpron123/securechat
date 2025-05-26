package org.project.securechat.sharedClass;


import java.lang.Runnable;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;
import java.io.PrintWriter;

public class Sender implements Runnable{
  private PrintWriter out;
  private BlockingQueue<String> queue;
  Function<String,String> procesing;
  public Sender(PrintWriter out,BlockingQueue<String> queue,Function<String,String> procesing){
    this.out =out;
    this.queue=queue;
    this.procesing=(s) -> s;
  }
  @Override
  public void run(){
    try{
      while(true){
        String message=queue.take();
        String processedMessage=procesing.apply(message);
        out.println(processedMessage);
      }
    }
    catch(InterruptedException e){
      System.out.println(e);
    }
  }
}