package org.project.securechat.server;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.lang.Runnable;

import org.project.securechat.sharedClass.Receiver;


public class ClientHandler implements Runnable{
    public Socket socket;

    PrintWriter out;
    public String userID;
    
    BlockingQueue<String> queue;

    public ClientHandler(Socket socket,String userID, BlockingQueue<String> oldqueue, PrintWriter out){
        this.socket = socket;
        this.userID= userID;
        this.queue=oldqueue;
        this.out=out;
    }
    public String getLogin(){
      return userID;
    }
    void processMessage(String command){
      System.out.println("Otrzymałem tą wiadomość: "+command);
    }
    @Override
    public void run(){
      try{
        System.out.println("Starting ClientHandler: "+userID);
        String message=null;
        while(true){
          message=queue.take();
          processMessage(message);
          out.println(message);
          }
        }
        catch(InterruptedException e){
          System.out.println(e);
        }finally{
          try{
            System.out.println("ClientHandler for "+userID+" closing socket.");
            socket.close();
            Server.getInstance().removeClient(userID);
          }catch(IOException e){
            e.printStackTrace();
          }
        }
    }
}
 