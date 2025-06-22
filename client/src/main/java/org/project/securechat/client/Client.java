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
  static BlockingQueue<String> status = new LinkedBlockingQueue<>();
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
        if (response != null && response.equals("OK")) {
          LOGGER.info("LOGOWANIE UDANE");
          ClientListener cListener = new ClientListener(clientOutputQueue, executor, userInput);
          executor.submit(cListener);

          // userInput.close();
          Message mess;
          while (!executor.isTerminated()){
            mess=serverInputQueue.take();
            if(commandHandlers.containsKey(mess.getDataType()))
              commandHandlers.get(mess.getDataType()).accept(mess);
          }
          break;

        }
      }

      // LOGGER.info("logowanie nie udane");
      try {
        socket.close();
        in.close();
        out.close();
      } catch (IOException d) {
        LOGGER.error("CANNOT CLOSE STREAMS", d);
      }
    } catch (Exception e) {
      e.printStackTrace();
      try {
        socket.close();
        in.close();
        out.close();
      } catch (IOException g) {
        e.printStackTrace();
      }

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
        LOGGER.info("creting chat: {} with aes: {}",chatid,aes);
        SqlHandlerConversations.Create_2_chat(chatid, Client.myID, senderId, aes);
      } catch (Exception e) {
        LOGGER.error("An error occurred: {}", e.getMessage(), e);
      }
     
    });
  
    }
  public static void main(String[] args) {
    Client client = new Client();
    client.start();
  }
}
