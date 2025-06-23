package org.project.securechat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.project.securechat.server.sql.SqlHandlerPasswords;


/**
 * Handles the login process for a client.
 * 
 */
public class Login implements Runnable {
  private Socket clientSocket;
  private DataInputStream in;
  private DataOutputStream out;
  private static final Logger LOGGER = LogManager.getLogger();
  private BlockingQueue<String> preClientInputQueue = new LinkedBlockingDeque<>(10);// wczesna kolejka user inputu
  private String currentLogin = null;
  private boolean firstTime = false;
  ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
  Login(Socket socket) {
    this.clientSocket = socket;
    try {
      in = new DataInputStream(socket.getInputStream());
      out = new DataOutputStream(socket.getOutputStream());
    } catch (IOException e) {

      LOGGER.error("Error setting up streams", e);
    }
  }

  private ServerReceiver receiver;
  /**
   * Checks if the given login and password match the stored values.
   * 
   * @param login  the login to check
   * @param password  the password to check
   * @return  whether the login and password match the stored values
   */
  Boolean correctpass(String login, String password) {
    return SqlHandlerPasswords.getUserPassword(login).equals(password);
  }
/**
 * Main loop of the login process. It manages the login and registration attempts for a client.
 * 
 * The user has 3 attempts to successfully log in or register. If the authentication is successful,
 * the main client handler is started. If all attempts fail, the connection is closed.
 * 
 * This method is executed as part of the Runnable interface implementation.
 * 
 */
  @Override
  public void run() {
    try {

      // --- Faza logowania/rejestracji ---
      // Użytkownik ma 3 próby na poprawne zalogowanie lub zarejestrowanie się
      for (int attempt = 1; attempt <= 3; attempt++) {
        currentLogin = handleAuthenticationAttempt(attempt);
        if (currentLogin != null) {
          LOGGER.info("Użytkownik {} zalogowany pomyślnie.", currentLogin);
          startMainClientHandler(currentLogin);
          return; // Zakończ wątek po udanym zalogowaniu i uruchomieniu handlera
        } else {
          // Jeśli to ostatnia próba i nadal się nie zalogował, zamknij połączenie
          if (attempt == 3) {
            out.writeUTF("Too many failed login/registration attempts. Connection closed.");
             out.flush();
            LOGGER.warn("Zbyt wiele nieudanych prób uwierzytelnienia dla klienta {}. Zamykam połączenie.",
                clientSocket.getInetAddress());
            clientSocket.close();
            return; // Zakończ wątek
          }
          out.writeUTF("Please try again. Remaining attempts: " + (3 - attempt));
           out.flush();
        }
      }

    } catch (InterruptedException e) {
      LOGGER.warn("Wątek uwierzytelniania został przerwany dla klienta {}.", clientSocket.getInetAddress(), e);
      Thread.currentThread().interrupt();
    } catch (IOException e) {
      LOGGER.error("Błąd I/O podczas uwierzytelniania dla klienta {}: {}", clientSocket.getInetAddress(),
          e.getMessage(), e);
    } finally {
      // Zamknij socket tutaj, jeśli nie został przejęty przez ClientHandler
      try {
        if (clientSocket != null && !clientSocket.isClosed() && currentLogin == null) {
          clientSocket.close();
          LOGGER.info("Połączenie z klientem {} zamknięte w finally.", clientSocket.getInetAddress());
        }
      } catch (IOException e) {
        LOGGER.error("Błąd podczas zamykania socketu w finally: {}", e.getMessage(), e);
      }
    }
  }

  /**
   * Handles a single login or registration attempt.
   * 
   * @param attemptNumber Current attempt number.
   * @return Successfully logged in user's login or null if login failed.
   * @throws IOException          If an I/O error occurs during communication.
   * @throws InterruptedException If the thread is interrupted while waiting for
   *                              data.
   */
  private String handleAuthenticationAttempt(int attemptNumber) throws IOException, InterruptedException {
    String loginAttempt = null;

    out.writeUTF("Enter login:");
     out.flush();
    loginAttempt = in.readUTF();
    if(Server.userActive(SqlHandlerPasswords.getUserId(loginAttempt))){
      LOGGER.error("User {} already logged in", loginAttempt);
      out.writeUTF("User already logged in.");
        out.flush();
      return null;
    }else{
      LOGGER.info("Attempt {} - received login {} from client {}", attemptNumber, loginAttempt,
      clientSocket.getInetAddress());
    }

    if (SqlHandlerPasswords.getUserId(loginAttempt)==-1) {
      LOGGER.info("Login {} not found in database.", loginAttempt);
      out.writeUTF("Login not found.");
       out.flush();
      out.writeUTF("Do you want to register? [type /register]");
        out.flush();
      String registerResponse = in.readUTF();
      if (registerResponse.trim().equalsIgnoreCase("/register")) {
        if (handleRegistration(loginAttempt)) {
          out.writeUTF("Registration successful.");
           out.flush();
          // Po rejestracji, spróbujemy od razu zalogować nowym loginem
          return loginAttempt;
        } else {
          out.writeUTF("Registration failed or login already exists.");
           out.flush();
          return null; // Rejestracja nieudana
        }
      } else {
        out.writeUTF("Invalid command or registration declined.");
         out.flush();
        return null; // Klient nie chce się zarejestrować
      }
    }

    // Jeśli login istnieje, spróbuj zalogować hasłem
    return attemptLoginWithPassword(loginAttempt);
  }
  /**
   * Handles a registration attempt for a new user.
   * 
   * <p>
   * Prompts the user to enter a new password and confirms it. If the passwords
   * match and the username does not exist in the database, the user is registered.
   * </p>
   * 
   * @param newLogin The login name for the new user.
   * @return true if registration is successful, false otherwise.
   * @throws IOException          If an I/O error occurs during communication.
   * @throws InterruptedException If the thread is interrupted while waiting for input.
   */
  private boolean handleRegistration(String newLogin) throws IOException, InterruptedException {
    LOGGER.info("Client attempting registration.");
    
    out.writeUTF("Enter new password:");
     out.flush();
     String newPassword = in.readUTF();
     
    out.writeUTF("Enter same new password:");
    out.flush();
    String newPassword2 = in.readUTF();
    
    if (!newPassword.equals(newPassword2)) {
      out.writeUTF("Passwords do not match. Please try again.");
       out.flush();
      return false;
    }


    if (SqlHandlerPasswords.getUserId(newLogin)==-1) {
      if (SqlHandlerPasswords.insertUser(newLogin, newPassword)) {
        //TODO insertUser return long
        LOGGER.info("New user registered: {}", newLogin);
        firstTime = true;
        return true;

      } else {
        LOGGER.error("Error inserting new user {} into database.", newLogin);
        return false;
      }
    } else {
      out.writeUTF("Login '" + newLogin + "' already exists. Please choose a different one.");
       out.flush();
      LOGGER.warn("Attempt to register existing login: {}", newLogin);
      return false;
    }
  }

  /**
   * Attempts to log in using the provided login name and the password entered
   * by the user.
   * 
   * <p>
   * The user has 3 attempts to enter the correct password. If the password is
   * correct, the method returns the login name of the user. If all attempts fail,
   * the method returns null.
   * </p>
   * 
   * @param login The login name to attempt to log in with.
   * @return The login name if the login is successful, null otherwise.
   * @throws IOException          If an I/O error occurs during communication.
   * @throws InterruptedException If the thread is interrupted while waiting for input.
   */
  private String attemptLoginWithPassword(String login) throws IOException, InterruptedException {
    if (login == null) { // Jeśli poprzedni etap (np. rejestracja) zakończył się błędem
      return null;
    }

    for (int i = 0; i < 3; i++) {
      out.writeUTF("Enter Password: ");
       out.flush();
      String password = in.readUTF();

      String storedPassword = SqlHandlerPasswords.getUserPassword(login);

      // WAŻNE: W prawdziwej aplikacji porównuj hasze, nie jawne hasła!
      if (storedPassword != null && storedPassword.equals(password)) {
        return login; // Zalogowano pomyślnie
      } else {
        out.writeUTF("Wrong Password. Attempt " + (i + 1) + "/3.");
         out.flush();
        LOGGER.warn("Incorrect password for user {}. Attempt {}/3.", login, (i + 1));
      }
    }
    out.writeUTF("Too many incorrect password attempts. Connection closed.");
     out.flush();
    LOGGER.warn("Too many failed password attempts for user {}. Connection closed.", login);
    clientSocket.close(); // Zamknij połączenie po 3 nieudanych próbach
    return null;
  }

  /**
   * Starts the main client handler for the newly logged in user.
   * 
   * <p>
   * This method is called after a successful login or registration attempt. It
   * starts a new {@link ClientHandler} to handle communication with the client.
   * </p>
   * 
   * @param login The login name of the newly logged in user.
   * @throws IOException          If an I/O error occurs during communication.
   * @throws InterruptedException If the thread is interrupted while waiting for input.
   */
  private void startMainClientHandler(String login) throws IOException, InterruptedException {
    // Zakładam, że ClientHandler ma konstruktor(Socket, String login,
    // BlockingQueue, DataOutputStream)
    long user_id=SqlHandlerPasswords.getUserId(login);
    if(firstTime == true || SqlHandlerPasswords.getPublicKey(user_id) == null){

      LOGGER.info("WYSYLAM WIADOMOSC Z PROSBA O KLUCZ");
      out.writeUTF("RSA_EXCHANGE");
      out.flush();
      Thread.sleep(200);
      String key = in.readUTF();
      LOGGER.info("ODEBRALEM WIADOMOSC {}",key);

      if(key.startsWith("RSA_EXCHANGE")){
        String rsakey = key.split(";")[1];
        if(SqlHandlerPasswords.updateKey(login,rsakey)){
          LOGGER.info("KLUCZ DODANY");
        }else{
          LOGGER.info("KLUCZ NIEDODANY ERROR ALBO UZYTKOWNIK NIE ISTNIEJE ALBO KLUCZ JUZ JEST");
        }
      }
     
    }

    long userid=SqlHandlerPasswords.getUserId(login);
    out.writeUTF("Welcome;"+login+";"+userid);
    out.flush();
    
    
    // start receiver here
    receiver = new ServerReceiver(in, preClientInputQueue,executor);
    executor.submit(receiver);
    LOGGER.info("ClientReceiver thread started for client: {}", clientSocket.getInetAddress());

    ClientHandler handler = new ClientHandler(clientSocket, userid, preClientInputQueue, out,executor);
    Server.getInstance().addClient(handler);
    
    executor.submit(handler);
    LOGGER.info("Main ClientHandler started for user: {}", login);
    LOGGER.info("watek LOGIN przerwany" );
  }
}