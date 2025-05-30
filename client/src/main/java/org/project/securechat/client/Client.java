package org.project.securechat.client;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.project.securechat.sharedClass.Receiver;
import org.project.securechat.sharedClass.Sender;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class Client{
  private static final String SERVER_HOST = "localhost";
  private static final int SERVER_PORT = 12345;
  private static final Logger LOGGER = LogManager.getLogger();

  private BlockingQueue<String> serverInputQueue=new LinkedBlockingDeque<>(10);// takes server messages
  private BlockingQueue<String> clientOutputQueue=new LinkedBlockingDeque<>(10);// takes client messages
  static BlockingQueue<String> status = new LinkedBlockingQueue<>();
  private Socket socket;
  ExecutorService executor = Executors.newFixedThreadPool(3);
  private DataOutputStream out;
  private DataInputStream in;
  static public String login;
  
  private class Processor implements Function<String,String>{
    @Override
    public String apply(String t) {
      return t;
    }
  }
  public void start(){
    try{
      socket=new Socket(SERVER_HOST,SERVER_PORT);
      out=new DataOutputStream(socket.getOutputStream());
      in = new DataInputStream(socket.getInputStream());

     
      ClientSender cSender=new ClientSender(out, clientOutputQueue,executor);
     
      ClientReceiver cReceiver=new ClientReceiver(in, serverInputQueue,clientOutputQueue,executor);
        
      executor.submit(cSender);
      executor.submit(cReceiver);
      // logowanie
      Scanner userInput=new Scanner(System.in);
      login=userInput.nextLine();
      clientOutputQueue.put(login);

      
      for(int i=0;i<3;i++){
       //enter password
     
        String password=userInput.nextLine();
    
        clientOutputQueue.put(password);
        Thread.sleep(50);
        String response = Client.status.poll();
        LOGGER.info("Status {}",response);
        if(response != null && response.equals( "OK")){
           LOGGER.info("LOGOWANIE UDANE");
          ClientListener cListener = new ClientListener(clientOutputQueue,executor);
          executor.submit(cListener);
          
          //userInput.close();
          while(!executor.isTerminated());
          break;
          
         
        }
      }
     //LOGGER.info("logowanie nie udane");
     try{
            socket.close();
        in.close();
        out.close();
     }
      catch(IOException d){
        LOGGER.error("CANNOT CLOSE STREAMS",d);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      try {
        socket.close();
        in.close();
        out.close();
      } catch (IOException g) {
        e.printStackTrace();
      }
        
      }
    
    
    
  }
  public static void main(String[] args){
    Client client=new Client();
    client.start();
  }
}
