package org.project.securechat.server;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.project.securechat.server.sql.*;
import org.project.securechat.sharedClass.Message;

/**
 * Main class for the server,
 * implemented as singleton.
 * <p>
 * It creates a ServerSocket and waits for incoming connections. When a new
 * connection is established, it creates a new Login which attempt login and start ClientHandler Thread
 * Server is responsible for storing all active clients and
 * provides methods for checking user is active.
 * </p>
 */
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
  /**
   * check if user is active
   * @param id of user
   * @return true if user is active, false otherwise
   */
  static public boolean userActive(Long id){
    if(clients.get(id) != null){
      return true;
    }else{
      return false;
    }
  }
  /**
   * adding client to container
   * @param client ClientHandler of new logged user
   */
  void addClient(ClientHandler client) {
    clients.put(client.getID(), client);
  }
  /**
   * remove user from container
   * @param id of user
   */
  void removeClient(Long id){
    clients.remove(id);
  }
  /**
   * Main loop of the server.
   * 
   */
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
  /**
   * send message to user
   * @param id of user
   * @param mess message to send
   */
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
