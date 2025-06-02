package org.project.securechat.server;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import org.project.securechat.sharedClass.Message;
import java.util.Map;

// import org.apache.logging.log4j.LogManager;
// import org.apache.logging.log4j.Logger;
import org.project.securechat.sharedClass.Receiver;


public class Server {
  private static final Server instance=new Server();
  private ServerSocket serverSocket;
  // private static final Logger LOGGER = LogManager.getLogger(); 
  private static final int PORT = 12345;
  public final Map<String,ClientHandler> clients = new ConcurrentHashMap<>();
  private AtomicBoolean running=new AtomicBoolean(true);

  private Server(){}
  public static Server getInstance(){
    return instance;
  }
  
  void addClient(String login,ClientHandler client){
    clients.put(login,client);
  }
  void removeClient(String login){
    clients.remove(login);
  }
  public void broadcastMessage(Message message){
    for(ClientHandler client: clients.values()){
      if(!client.userID.equals(message.getSenderID())){
        try{
        client.Outputqueue.put(message);
        }catch(InterruptedException e){
          System.err.println("Failed to put message in "+client.userID+"'s queue");
        }
      }
    }
  }

  private void start(){
    try{
      serverSocket = new ServerSocket(PORT);
      System.out.println("Server started on port " + PORT);
      while (running.get()) {
        Socket clientSocket = serverSocket.accept();
        System.out.println("New client connected");
        Login loginHandler = new Login(clientSocket);
        new Thread(loginHandler).start();
        }
    }catch (IOException e) {
      e.printStackTrace();
    }finally{
      if(serverSocket != null && !serverSocket.isClosed())
        try{
          serverSocket.close();
        }catch(IOException e){
          System.err.println("Error closing server socket");
        }
    }
  }


  // private void shutdown(){} TODO

  ClientHandler getClientHandler(String login){
    return clients.get(login);
  }

  private class Login implements Runnable{
    private final Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    
    private BlockingQueue<Message> preOutputQueue =new LinkedBlockingDeque<>(10);
    
    private String login;
    Login(Socket socket){
      this.socket = socket;
      try{
        this.in=new DataInputStream(socket.getInputStream());
        this.out=new DataOutputStream(socket.getOutputStream());
      }catch(IOException e){
        System.out.println("Error setting up streams: "+e);
      }
    }
    static HashMap<String,String> loginsAndPass=new HashMap<>();
    static {
      loginsAndPass.put("Jan","123");
      loginsAndPass.put("Ola","ola");
      loginsAndPass.put("Piotr","piotrek");
    }
    Boolean correctpass(String login,String password){
      return loginsAndPass.containsKey(login) && loginsAndPass.get(login).equals(password);
    }

    @Override
    public void run() {
      try{
        login=null;
        //pytaine o logowanie
        out.writeUTF("Enter Login: ");
        out.flush();

        login=in.readUTF();
        System.out.println("Received login from "+socket.getInetAddress().getHostAddress()+" : "+login);
        
        if(!loginsAndPass.containsKey(login)){
          out.writeUTF("login not found");
          out.writeUTF("Do u want to register? (y/n) [not implemented]");
          // TODO
          return;
        }
        // 3 krotna proba podania hasla
        for(int i=0;i<3;i++){
          out.writeUTF("Enter Password: ");
          String password=in.readUTF();
          if(correctpass(login,password)){
            out.writeUTF("Welcome "+login);
            out.flush();
            ClientHandler handler=new ClientHandler(socket, login, preOutputQueue, in, out);
            Server.getInstance().addClient(login, handler);
            return;
          }else{
            out.writeUTF("Wrong Password");
            out.flush();
            if(i==2){
              System.out.println("too many attempts.");
              out.writeUTF("Too many attempts. Connection closed.");
              out.flush();
              return;
            }
          }
        }
      }catch(IOException e){
        System.err.println("Login error");
      }finally{
        // TODO make method cleanup for closing everything
        if (socket != null && !socket.isClosed() && (login == null || !Server.getInstance().clients.containsKey(login))) {
          try {
              System.out.println("LoginHandler closing socket for failed/abandoned login attempt " + socket.getPort());
              if (in != null) in.close();
              if (out != null) out.close();
              socket.close();
          } catch (IOException e) {
              System.err.println("Error closing socket after failed login: " + e.getMessage());
          }
        }
      }
    }
  }
  public static void main(String[]args){
    Server server = Server.getInstance();
    server.start();
  }
}

