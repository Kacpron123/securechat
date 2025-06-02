package org.project.securechat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.lang.Runnable;
import java.util.function.Function;

import org.project.securechat.sharedClass.Receiver;
import org.project.securechat.sharedClass.Message;


public class ClientHandler implements Runnable{
  public Socket socket;
  private DataOutputStream out;
  private DataInputStream in;
  public String userID;
  private volatile boolean running=true;
  BlockingQueue<Message> Outputqueue;

  private Receiver receiver; // Reference to the receiver thread's Runnable
  private Thread receiverThread; // The actual Thread object for the receiver

  private class IncomingMessageProcessor implements Function<Message,Message>{
    @Override
    public Message apply(Message message) {
      Message.MessageTYPE type=message.getMessageTYPE();
      if(type==Message.MessageTYPE.COMMAND){
        if(message.getData().equals("/exit")){
          running=false;
          return null;
        }
      }
      return null;
    }
  }
  
  public ClientHandler(Socket socket,String userID, BlockingQueue<Message> oldOutputqueue) throws IOException{
    this.socket = socket;
    this.userID= userID;
    this.Outputqueue=oldOutputqueue;
    this.in = new DataInputStream(socket.getInputStream());
    this.out = new DataOutputStream(socket.getOutputStream());

    // Initialize and start the receiver thread
    this.receiver = new Receiver(this.in, new LinkedBlockingDeque<>(), new IncomingMessageProcessor()); // Receiver will put *processed* messages here
    this.receiverThread = new Thread(receiver, "Receiver-" + userID);
    this.receiverThread.start(); // Start listening for incoming messages
  }
  public String getLogin(){
    return userID;
  }
  public void stopRunning(){
    this.running = false; // Signal ClientHandler's main loop to stop
    if (receiver != null) {
      receiver.stopRunning(); // Signal Receiver to stop
    }
    // Interrupt the ClientHandler's thread if it's blocked on something (e.g., outputQueue.take())
    Thread.currentThread().interrupt();
  }
  @Override
  public void run(){
    try{
      while(running){
        Message message = Outputqueue.take();
        out.writeUTF(Message.toJSON(message));
      }
    }
    new Thread
    catch(InterruptedException e){
      System.out.println("CLientHandler interrupted [Shouting down]: "+e.getMessage());
      Thread.currentThread().interrupt();
    }catch(IOException e){
      System.out.println("Error sending message: "+e.getMessage());
    }finally{
      try{
        System.out.println("ClientHandler for "+userID+" closing socket.");
        socket.close();
        Server.getInstance().removeClient(userID);
      }catch(IOException e){
        e.printStackTrace();
      }finally{
        cleanup();
      }
    }
  }
  private void cleanup() {
    // First, gracefully stop the receiver thread
    if (receiverThread != null && receiverThread.isAlive()) {
      System.out.println("Attempting to stop receiver thread for " + userID + "...");
      receiver.stopRunning(); // Signal receiver to stop its loop
      receiverThread.interrupt(); // Interrupt receiver if blocked (e.g., on in.readInt())
      try {
        receiverThread.join(2000); // Wait for receiver to finish, with a timeout
        if (receiverThread.isAlive()) {
          System.err.println("Receiver thread for " + userID + " did not terminate gracefully after join.");
        }
      } catch (InterruptedException ie) {
        System.err.println("ClientHandler interrupted while waiting for receiver thread to join.");
        Thread.currentThread().interrupt(); // Restore interrupt status
      }
    }

    // Now, close the streams and socket
    try {
      System.out.println("ClientHandler for " + userID + " closing socket and streams.");
      // Close streams first. Closing either input or output stream
      // associated with a socket will close the entire socket.
      if (out != null) out.close();
      if (in != null) in.close();
      if (socket != null && !socket.isClosed()) socket.close(); // Just a safeguard
      Server.getInstance().removeClient(userID);
      System.out.println("ClientHandler cleanup complete for " + userID + ".");
    } catch (IOException e) {
      System.err.println("Error during client handler cleanup for " + userID + ": " + e.getMessage());
    }
  }
}