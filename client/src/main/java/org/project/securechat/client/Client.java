package org.project.securechat.client;

import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.project.securechat.sharedClass.Receiver;
import org.project.securechat.sharedClass.Message;
import org.project.securechat.sharedClass.ShutdownSignal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Client implements ShutdownSignal{
  private static final String SERVER_HOST = "localhost";
  private static final int SERVER_PORT = 12345;
  private static final String ServerChatID ="Server";

  private Socket socket;
  
  private Scanner scanner;
  private DataOutputStream out;
  private DataInputStream in;
  private String login;
  
  private AtomicBoolean running=new AtomicBoolean(true);
  private BlockingQueue<Message> clientOutputQueue=new LinkedBlockingDeque<>(10);// takes client messages
  
  private TerminalListener terminalListener;
  private Receiver receiver;
  private Thread terminalListenerThread;
  private Thread receiverThread;  

  // TODO add initializeShutDown
  // Client get /exit then send /exit to ClientHandler and stop Thread
  public Client(Socket socket) throws IOException {
      this.socket = socket;
      this.out = new DataOutputStream(socket.getOutputStream());
      this.in = new DataInputStream(socket.getInputStream());
      // For testing, mockSocket.getInetAddress() and getPort() will be mocked in the test
      System.out.println("Connected to " + socket.getInetAddress().getHostName() + ":" + socket.getPort());

      scanner = new Scanner(System.in);
      login();

      this.terminalListener = new TerminalListener(scanner, clientOutputQueue, login, this);
      this.terminalListenerThread = new Thread(terminalListener);
      this.terminalListenerThread.start();

      this.receiver = new Receiver(in, clientOutputQueue, new Processor(),this);
      this.receiverThread = new Thread(receiver);
      this.receiverThread.start();
  }
  public Client() throws IOException{
    this(new Socket(SERVER_HOST,SERVER_PORT));
  }

  public void initiateShutDown() {
    System.out.println("Client: Initiating shutdown process...");
    running.set(false); // konczenie petli Client; TerminalListener i Receiver 

    if(terminalListener != null)
      terminalListener.stopRunning();

    if(receiver!=null)
      receiver.stopRunning();

    try{
      System.out.println("Client: Closing socket to unblock Receiver.");
      if(socket!=null && !socket.isClosed()){
        socket.close();
      }
    }catch(IOException e){
      System.out.println("Client: Error closing socket during shutdown: " + e.getMessage());
    }
    if(terminalListenerThread!=null){
      terminalListenerThread.interrupt();
    }
    if(receiverThread!=null){
      receiverThread.interrupt();
    }

  }
  private void login() throws IOException{
    // login
    System.out.println(in.readUTF());
    String login=scanner.nextLine();
    this.login=login;
    out.writeUTF(login);
    
    String response=null;
    for(int i=0;i<3;i++){
      response = in.readUTF(); //enter password
      String password=scanner.nextLine();
      out.writeUTF(password);
      response = in.readUTF();
      if(response.startsWith("Welcome")){
        return;
      }
    }
    throw new IOException("Too many attempts");
  }
  private class Processor implements Function<Message,Message>{
    @Override
    public Message apply(Message t) {
      if(t.getMessageType().equals(Message.MessageTYPE.COMMAND)){
        if(t.getData().equals("/exit")){
          running.set(false);
          clientOutputQueue.add(new Message(login,ServerChatID,Message.MessageTYPE.COMMAND,"/exit_back"));
          initiateShutDown();
        }
        if(t.getData().equals("/exit_back")){
          running.set(false);
          cleanup();
        }
      }
      return t;
    }
  }
  public void start(){
    System.out.println("Client MainLoop started");
    while(running.get()){
      try{
        Message msgToSend=clientOutputQueue.take();
        if(msgToSend!=null)
          out.writeUTF(Message.toJSON(msgToSend));
      }catch(InterruptedException | IOException e){
        System.out.println("Client MainLoop interrupted [Shouting down]: "+e.getMessage());
        running.set(false);
      }
    }
    System.out.println("Client MainLoop finished");
    cleanup();
  }
  public void cleanup(){
    System.out.println("Client: Initiating cleanup.");
    // 1. Zamknięcie TerminalListener
    if (terminalListener != null) {
      terminalListener.stopRunning();
      terminalListenerThread.interrupt();
      try {
        terminalListenerThread.join(1000);
        if (terminalListenerThread.isAlive()) {
          System.err.println("Client: TerminalListener thread did not terminate gracefully.");
        }
      } catch (InterruptedException e) {
        System.err.println("Client: Interrupted while waiting for TerminalListener.");
        Thread.currentThread().interrupt();
      }
    }

    // 2. Zamknięcie Receiver
    if (receiver != null) {
      receiver.stopRunning();
      receiverThread.interrupt(); // Odblokuje, jeśli czeka na BlockingQueue.put()
      // WAŻNE: Zamknięcie socketu ODRAZU, aby odblokować Receiver z DataInputStream.readUTF()
      try {
        System.out.println("Client: Closing socket to unblock Receiver.");
        if (socket != null && !socket.isClosed()) {
          // Zamknięcie socketu zamknie powiązane strumienie (in, out)
          socket.close();
        }
      } catch (IOException e) {
        System.err.println("Client: Error closing socket during cleanup: " + e.getMessage());
      }

      try {
        receiverThread.join(1000);
        if (receiverThread.isAlive()) {
          System.err.println("Client: Receiver thread did not terminate gracefully.");
        }
      } catch (InterruptedException e) {
        System.err.println("Client: Interrupted while waiting for Receiver.");
        Thread.currentThread().interrupt();
      }
    }

    // 3. Upewnij się, że strumienie i socket są zamknięte, jeśli nie zostały wcześniej
    // Jest to nadmiarowe, jeśli socket.close() powyżej zawsze się wykona,
    // ale zabezpiecza na wypadek np. wyjątku w `receiver.stopRunning()`
    try {
      if (out != null) out.close(); // Zamknięcie out, jeśli jeszcze otwarte
      if (in != null) in.close();   // Zamknięcie in, jeśli jeszcze otwarte
      if (socket != null && !socket.isClosed()) socket.close(); // Upewnij się, że socket jest zamknięty
      System.out.println("Client: All network resources closed.");
    } catch (IOException e) {
      System.err.println("Client: Error during final network resource cleanup: " + e.getMessage());
    }

    // Na koniec zamknij skaner, który został otwarty w Client
    if (scanner != null) {
      scanner.close();
      System.out.println("Client: Scanner (System.in) closed.");
    }
    System.out.println("Client: Cleanup complete. Program exiting.");
  }
  public static void main(String[] args){
    try{
      Client client=new Client();
      client.start();
    }catch(IOException e){
      System.out.println("Error: "+e.getMessage());
    }
  }
}