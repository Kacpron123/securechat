package org.project.securechat.client;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.project.securechat.client.sql.SqlHandlerConversations;
import org.project.securechat.client.sql.SqlHandlerMessages;
import org.project.securechat.client.sql.SqlHandlerFriends;
import org.project.securechat.sharedClass.JsonConverter;
import org.project.securechat.sharedClass.Message;
import org.project.securechat.sharedClass.Message.DataType;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main class for the client.
 * 
 * It is responsible for connecting to the server, sending messages, receiving
 * messages and handling user input.
 * 
 */
public class Client {
  private static final String SERVER_HOST = "localhost";
  private static final int SERVER_PORT = 12345;
  private static final Logger LOGGER = LogManager.getLogger();
  private final Map<DataType, Consumer<Message>> commandHandlers = new HashMap<>();
  private BlockingQueue<Message> serverInputQueue = new LinkedBlockingDeque<>(10);// takes server messages
  private BlockingQueue<String> clientOutputQueue = new LinkedBlockingDeque<>(10);// takes client messages
  static BlockingQueue<String> status = new LinkedBlockingQueue<>(1);
  private Socket socket;
  ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
  private DataOutputStream out;
  private DataInputStream in;
  private BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8));
  static public String login;
  static public long myID;
  public Client(){
    // SQLs:
    SqlHandlerConversations.createChatRelated();
    SqlHandlerFriends.createRsaTable();
    SqlHandlerMessages.createMessagesTable();
    initCommandHandlers();
  }
  /**
   * Starts the client by connecting to the server, sending messages, receiving
   * messages and handling user input.
   */
  public void start() {
    try {
      socket = new Socket(SERVER_HOST, SERVER_PORT);
      out = new DataOutputStream(socket.getOutputStream());
      in = new DataInputStream(socket.getInputStream());

      ClientSender cSender = new ClientSender(out, clientOutputQueue, executor);

      ClientReceiver cReceiver = new ClientReceiver(in, serverInputQueue, clientOutputQueue, executor);

      executor.submit(cSender);
      executor.submit(cReceiver);
      
      // login
      while (!executor.isTerminated()) {

        String messageForServer = userInput.readLine();
        if (messageForServer.equals("/exit")) {
          executor.shutdownNow();
          userInput.close();
          break;
        }
        clientOutputQueue.put(messageForServer);
        Thread.sleep(2000);
        String response = Client.status.poll();
        // TODO: checkinng error and aborting
        // correct logged
        if (response != null && response.equals("OK")){
          LOGGER.info("LOGOWANIE UDANE");
          ClientListener cListener = new ClientListener(clientOutputQueue, executor, userInput);
          executor.submit(cListener);

          // main loop
          Message mess;
          while (!executor.isTerminated()){
            mess=serverInputQueue.poll(20,TimeUnit.MILLISECONDS);
            if(mess!=null && commandHandlers.containsKey(mess.getDataType()))
              commandHandlers.get(mess.getDataType()).accept(mess);
          }
          break;

        }
      }

    } catch (InterruptedException e) {
      LOGGER.info("Client main thread interrupted during operation.", e);
      Thread.currentThread().interrupt(); // Re-interrupt
    } catch (IOException e) {
        LOGGER.error("Client I/O error during startup or main loop: {}", e.getMessage(), e);
    } catch (Exception e) {
      LOGGER.error("An unexpected error occurred in Client.start(): {}", e.getMessage(), e);
      e.printStackTrace(); // For unexpected exceptions
    }finally{
      // Ensure all resources are closed, even if errors occur
      LOGGER.info("Client shutting down...");
      executor.shutdown(); // Initiate graceful shutdown
      try {
          // Give threads time to terminate
          if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
              LOGGER.warn("Executor did not terminate gracefully, forcing shutdown.");
              executor.shutdownNow(); // Force shutdown if not graceful
          }
      } catch (InterruptedException e) {
          LOGGER.error("Error awaiting executor termination: {}", e.getMessage(), e);
          Thread.currentThread().interrupt();
      }

      try {
        if (socket != null && !socket.isClosed()) socket.close();
        if (in != null) in.close();
        if (out != null) out.close();
        if (userInput != null) userInput.close(); // Close userInput at the end of client life
      } catch (IOException d) {
        LOGGER.error("Error closing client streams/socket: {}", d.getMessage(), d);
      }
      LOGGER.info("Client shutdown complete.");
    }

  }
  /**
   * Initialize command handlers for handling commands received from the server.
   */
  private void initCommandHandlers() {
    // for now here
    // TODO: use CompletableFuture for handling long-running operations
    /**
     * reacting to getting information of create chat
     * message is constructed as:
     * senderID: id of creater 
     * chatID: id of created chat
     * DataType: CREATE_2_CHAT
     * Data: "aeskey"
     */
    commandHandlers.put(DataType.CREATE_2_CHAT,msg ->{
      long chatid=msg.getChatID();
      LOGGER.info("i get information about creating chat {}", chatid);
      long senderId = msg.getSenderID();
      try {
        if(!SqlHandlerFriends.checkIfUserIdExists(msg.getSenderID())){
          LOGGER.debug("don't have rsa, asking server");
          Message tosend = new Message(myID, 0, DataType.RSA_KEY, "ID:" + msg.getSenderID());
          out.writeUTF(JsonConverter.parseObjectToJson(tosend));
          Thread.sleep(200);
        }
      } catch (Exception e) {
        LOGGER.error("Error sending RSA_KEY message: {}", e.getMessage(), e);
      }
      try{
        PrivateKey privkey=EncryptionService.readPrivateKeyFromFile();
        String aes=EncryptionService.getString64FromBytes(EncryptionService.decodeWithRsa(privkey,msg.getData()));
        System.out.println("chat created");
        LOGGER.info("creting chat: {} with aes: {}",chatid,aes);
        SqlHandlerConversations.Create_2_chat(chatid, Client.myID, senderId, aes);
      } catch (Exception e) {
        LOGGER.error("An error occurred: {}", e.getMessage(), e);
      }
     
    });
  
    }
  /**
   * Main method to start the client.
   * @param args command line arguments
   */
  public static void main(String[] args) {
    Client client = new Client();
    client.start();
  }
}
