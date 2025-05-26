package org.project.securechat.server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.lang.Runnable;

 import org.apache.logging.log4j.LogManager;
 import org.apache.logging.log4j.Logger;
import org.project.securechat.sharedClass.Receiver;


public class ClientHandler implements Runnable{
  private static final Logger LOGGER = LogManager.getLogger(); 
    public Socket socket;
     
    DataOutputStream out;
    public String userID;
    
    BlockingQueue<String> clientInputQueue;

    public ClientHandler(Socket socket,String userID, BlockingQueue<String> preClientInputQueue, DataOutputStream out){
        this.socket = socket;
        this.userID= userID;
        this.clientInputQueue=preClientInputQueue;
        this.out=out;
    }
    public String getLogin(){
      return userID;
    }
    void processMessage(String command){
      LOGGER.info("Otrzymalem ta wiadomosc {} : {}",userID,command);
      
    }
    @Override
    public void run(){
      try{
        
        LOGGER.info("Starting ClientHandler: {}",userID);
      
        String message=null;
        while(true){
          message=clientInputQueue.take();
          processMessage(message);
          out.writeUTF(message);
          }
        }
        catch(InterruptedException |IOException e){
          System.out.println(e);
        }finally{
          try{
            
            LOGGER.info("ClientHandler for {} closing socket.",userID);
            socket.close();
            Server.getInstance().removeClient(userID);
          }catch(IOException e){
            e.printStackTrace();
          }
        }
    }
}
 