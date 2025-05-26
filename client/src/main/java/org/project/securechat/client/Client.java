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
  
  private BlockingQueue<String> queue=new LinkedBlockingDeque<>(10);
  private BlockingQueue<String> queueout=new LinkedBlockingDeque<>(10);
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

      Receiver receiver=new Receiver(in, queue);
      new Thread(receiver).start();
      Sender sender=new Sender(out, queueout, null);
      new Thread(sender).start();

      // logowanie
      Scanner scanner=new Scanner(System.in);
      login=scanner.nextLine();
      queueout.put(login);

      String response=null;
      for(int i=0;i<3;i++){
        response = queue.take(); //enter password
        String password=scanner.nextLine();
        queueout.put(password);
        response = queue.take();
        if(response.startsWith("Welcome")){
          while(true){
            String message=scanner.nextLine();
            queueout.put(message);
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
