package org.project.securechat.server;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

// import org.apache.logging.log4j.LogManager;
// import org.apache.logging.log4j.Logger;
import org.project.securechat.sharedClass.Receiver;


public class Server {
  private static Server instance=null;
  // private static final Logger LOGGER = LogManager.getLogger(); 
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
        System.out.println("New client connected");
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
    private BufferedReader in;
    private PrintWriter out;
    
    private BlockingQueue<String> queue =new LinkedBlockingDeque<>(10);
    
    Login(Socket socket){
      this.socket = socket;
      try{
        in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out=new PrintWriter(socket.getOutputStream(),true);
      }catch(IOException e){
        System.out.println("Error setting up streams: "+e);
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
        receiver=new Receiver(in, queue);
        new Thread(receiver).start();
        String login=null;
        //pytaine o logowanie
        out.println("Enter login: ");
        login=queue.take();
        System.out.println("Otrzymałem login: "+login);
        if(!loginsAndPass.containsKey(login)){
          out.println("login not found");
          out.println("Do u want to register? (y/n) [not implemented]");
          return;
        }
        // 3 krotna proba podania hasla
        for(int i=0;i<3;i++){
          out.println("Enter Password: ");
          String password=queue.take();
          if(correctpass(login,password)){
            out.println("Welcome "+login);
            break;
          }else{
            out.println("Wrong Password");
            if(i==2){
              System.out.println("too many attempts.");
              return;
            }
          }
        }
        System.out.println("Użytkownik zalogowany: "+login);
        ClientHandler handler = new ClientHandler(socket,login,queue,out);
        Server.getInstance().addClient(handler);
        new Thread(handler).start();
      } catch (InterruptedException e) {
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

