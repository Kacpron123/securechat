package org.project.securechat.server;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.project.securechat.server.sql.SqlHandlerPasswords;

public class Server {
  private static Server instance = null;
  private static final Logger LOGGER = LogManager.getLogger();
  private static final int PORT = 12345;
  public static final HashMap<String, ClientHandler> clients = new HashMap<>();

  private Server() {
  }

  public static Server getInstance() {
    if (instance == null) {
      instance = new Server();
    }
    return instance;
  }
  boolean userActive(String login){
    if(clients.get(login) != null){
      return true;
    }else{
      return false;
    }
  }
  boolean userExists(String login){
    String user = SqlHandlerPasswords.getPublicKey(login);
    if(user !=null){
      return true;
    }else{
      return false;
    }
  }
  void addClient(ClientHandler client) {
    clients.put(client.getLogin(), client);
  }

  void removeClient(String login) {
    clients.remove(login);
  }

  private void start() {
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      System.out.println("Server started on port " + PORT);
      while (true) {
        Socket clientSocket = serverSocket.accept();

        LOGGER.info("New client connected {}", clientSocket.getInetAddress());
        Login login = new Login(clientSocket);
        new Thread(login).start();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  static ClientHandler getSocket(String login) {
    return clients.get(login);
  }

  public static void main(String[] args) {
    Server server = Server.getInstance();
    server.start();
  }
}
