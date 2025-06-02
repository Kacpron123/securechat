package org.project.securechat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.Runnable;
import java.util.function.Function;

import org.project.securechat.sharedClass.Receiver;
import org.project.securechat.sharedClass.ShutdownSignal;
import org.project.securechat.sharedClass.Message;


public class ClientHandler implements Runnable, ShutdownSignal{
  private final Socket socket;
  private final DataOutputStream out;
  private final DataInputStream in;
  final String userID;
  AtomicBoolean running=new AtomicBoolean(true);
  BlockingQueue<Message> Outputqueue;

  private Receiver receiver; // Reference to the receiver thread's Runnable
  private Thread receiverThread; // The actual Thread object for the receiver

  private class IncomingMessageProcessor implements Function<Message,Message>{
    @Override
    public Message apply(Message message) {
      if(message==null){
        return null;
      }
      Message.MessageTYPE type=message.getMessageType();
      if(type==Message.MessageTYPE.COMMAND){
        if(message.getData().equals("/exit")){
          running.set(false);
          return new Message();
        }
      }
      return null;
    }
  }
  
  public ClientHandler(Socket socket,String userID, BlockingQueue<Message> oldOutputqueue,DataInputStream in, DataOutputStream out) throws IOException{
    this.socket = socket;
    this.userID= userID;
    this.Outputqueue=oldOutputqueue;
    this.in = in;
    this.out = out;

    // Initialize and start the receiver thread
    this.receiver = new Receiver(this.in, new LinkedBlockingDeque<>(), new IncomingMessageProcessor(),this);
    this.receiverThread = new Thread(receiver, "Receiver-" + userID);
    this.receiverThread.start(); // Start listening for incoming messages
    System.out.println("ClientHandler for "+userID+" initialized and started");
  }
  public void stopRunning(){
    this.running.set(false); // Signal ClientHandler's main loop to stop
  }


  @Override
  public void run(){
    try{
      while(running.get()){
        Message message = Outputqueue.take();
        out.writeUTF(Message.toJSON(message));
      }
    }
    catch(InterruptedException e){
      System.out.println("CLientHandler interrupted [Shouting down]: "+e.getMessage());
      Thread.currentThread().interrupt();
    }catch(IOException e){
      System.out.println("Error sending message: "+e.getMessage());
    }finally{
      cleanup();
    }
  }
  public void initiateShutDown(){
    stopRunning();
    Server.getInstance().removeClient(userID);
  }
  public void cleanup() {
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
    }finally{
      if(Server.getInstance()!=null)
        Server.getInstance().removeClient(userID);
    }
    System.out.println("ClientHandler complete work");
  }
  
}