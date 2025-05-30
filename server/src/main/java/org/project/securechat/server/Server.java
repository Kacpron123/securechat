package org.project.securechat.server;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

 import org.apache.logging.log4j.LogManager;
 import org.apache.logging.log4j.Logger;
import org.project.securechat.sharedClass.Receiver;


public class Server {
  private static Server instance=null;
  private static final Logger LOGGER = LogManager.getLogger(); 
  private static final int PORT = 12345;
  public final HashMap<String,ClientHandler> clients = new HashMap<>();

  private Server(){}
  public static Server getInstance(){
    if(instance==null){
      instance=new Server();
    }
    return instance;
  }
  void addClient(ClientHandler client){
    clients.put(client.getLogin(),client);
  }
  void removeClient(String login){
    clients.remove(login);
  }
  private void start(){
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      System.out.println("Server started on port " + PORT);
      while (true) {
        Socket clientSocket = serverSocket.accept();
        
        LOGGER.info("New client connected {}",clientSocket.getInetAddress());
        Login login = new Login(clientSocket);
        new Thread(login).start();
        }
    }catch (IOException e) {
      e.printStackTrace();
    }
  }
  ClientHandler getSocket(String login){
    return clients.get(login);
  }
  private class Login implements Runnable{
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    
    private BlockingQueue<String> preClientInputQueue =new LinkedBlockingDeque<>(10);// wczesna kolejka user inputu
    
    Login(Socket socket){
      this.socket = socket;
      try{
        in=new DataInputStream(socket.getInputStream());
        out=new DataOutputStream(socket.getOutputStream());
      }catch(IOException e){
        
        LOGGER.error("Error setting up streams",e);
      }
    }
    private Receiver receiver;
    static HashMap<String,String> loginsAndPass=new HashMap<>();
    static {
      loginsAndPass.put("Jan","123");
      loginsAndPass.put("Ola","ola");
      loginsAndPass.put("Piotr","piotrek");
    }
    Boolean correctpass(String login,String password){
      return loginsAndPass.get(login).equals(password);
    }
    @Override
    public void run() {
      try{
        receiver=new Receiver(in, preClientInputQueue);
        new Thread(receiver).start();
        String login=null;
        //pytaine o logowanie
        out.writeUTF("Enter login: ");
        login=preClientInputQueue.take();
        
        LOGGER.info("Otrzymalem login {}",login);
        if(!loginsAndPass.containsKey(login)){
          out.writeUTF("login not found");
          out.writeUTF("Do u want to register? (y/n) [not implemented]");
          return;
        }
        // 3 krotna proba podania hasla
        for(int i=0;i<3;i++){
          out.writeUTF("Enter Password: ");
          String password=preClientInputQueue.take();
          if(correctpass(login,password)){
            out.writeUTF("Welcome "+login);
            break;
          }else{
            out.writeUTF("Wrong Password");
            if(i==2){
              System.out.println("too many attempts.");
              socket.close();
              return;
            }
          }
        }
        
        LOGGER.info("Uzytkownik zalogowany: {}",login);
        ClientHandler handler = new ClientHandler(socket,login,preClientInputQueue,out);
        Server.getInstance().addClient(handler);
        new Thread(handler).start();
      } catch (InterruptedException | IOException e) {
        LOGGER.error("Server : run ",e);
        e.printStackTrace();
      }finally{
      }
    }
  }
  public static void main(String[]args){
    Server server = Server.getInstance();
    server.start();
  }
}

