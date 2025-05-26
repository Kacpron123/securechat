package org.project.securechat.sharedClass;
import java.time.Instant;

public class Message{
  public Message(String senderID, String chatID, String data){
    this.senderID = senderID;
    this.chatID = chatID;
    this.timestamp = Instant.now();
    this.data = data;
  }
  public String senderID;
  public String chatID;
  public Instant timestamp;
  // public byte[] message;
  public String data; //for now 
  public void write(){
    System.out.println(data);
  }
}
