package org.project.securechat.server;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.project.securechat.server.sql.*;
import org.project.securechat.sharedClass.Message;

public class Server {
  private static Server instance = null;
  private static final Logger LOGGER = LogManager.getLogger();
  private static final int PORT = 12345;
  public static final HashMap<Long, ClientHandler> clients = new HashMap<>();

  private Server() {
    SqlHandlerPasswords.createUsersTable();
    SqlHandlerConversations.createChatRelated();
    SqlHandlerMessages.createMessagesTable();
  }

  public static Server getInstance() {
    if (instance == null) {
      instance = new Server();
    }
    return instance;
  }
  static public boolean userActive(Long id){
    if(clients.get(id) != null){
      return true;
    }else{
      return false;
    }
  }
  boolean userExists(long id){
    if(SqlHandlerPasswords.getUsernameFromUserId(id) != null){
      return true;
    }else{
      return false;
    }
  }
  void addClient(ClientHandler client) {
    clients.put(client.getID(), client);
  }

  void removeClient(Long id){
    clients.remove(id);
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

  static void sendMessage(long id,Message mess){
    ClientHandler ch=clients.get(id);
    if(ch==null){
      LOGGER.info("user {} not online",id);
    }
    else
      ch.sendMessage(mess);
  }

  public static void main(String[] args) {
    Server server = Server.getInstance();
    server.start();
  }
}
