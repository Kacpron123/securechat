package org.project.securechat.client;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;

import org.project.securechat.sharedClass.Receiver;
import org.project.securechat.sharedClass.Sender;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class Client{
  private static final String SERVER_HOST = "localhost";
  private static final int SERVER_PORT = 12345;
  
  private BlockingQueue<String> serverInputQueue=new LinkedBlockingDeque<>(10);// takes server messages
  private BlockingQueue<String> clientOutputQueue=new LinkedBlockingDeque<>(10);// takes client messages
  private Socket socket;
  private DataOutputStream out;
  private DataInputStream in;
  private String login;
  
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

      Receiver receiver=new Receiver(in, serverInputQueue);
      new Thread(receiver).start();
      Sender sender=new Sender(out, clientOutputQueue, null);
      new Thread(sender).start();

      // logowanie
      Scanner userInput=new Scanner(System.in);
      login=userInput.nextLine();
      clientOutputQueue.put(login);

      String response=null;
      for(int i=0;i<3;i++){
        response = serverInputQueue.take(); //enter password
        String password=userInput.nextLine();
        clientOutputQueue.put(password);
        response = serverInputQueue.take();
        if(response.startsWith("Welcome")){
          while(true){
            String message=userInput.nextLine();
            clientOutputQueue.put(message);
          }
        }
      }

    }catch (Exception e) {
      e.printStackTrace();
    }finally{
      try {
        socket.close();
        
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
  public static void main(String[] args){
    Client client=new Client();
    client.start();
  }
}
