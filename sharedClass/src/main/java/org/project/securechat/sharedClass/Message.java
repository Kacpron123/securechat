package org.project.securechat.sharedClass;
import java.time.Instant;

import lombok.Getter;
import lombok.Setter;


public class Message{
  public Message(){}
  public Message(String senderID, String chatID,DataType dataType, String data){
    this.senderID = senderID;
    this.chatID = chatID;
    System.out.println("works fine");
    this.dataType = dataType;
    this.timestamp = Instant.now();
    System.out.println("works fine");
    this.data = data;
  }
  private String senderID;
  private String chatID;
  private Instant timestamp;
  private DataType dataType;

  public DataType getDataType() {
    return dataType;
  }
  public void setDataType(DataType dataType) {
    this.dataType = dataType;
  }
  // public byte[] message;
  private String data; //for now 
   public enum DataType {
    TEXT,
    FILE,
    KEY_EXCHANGE;
  }
  public void write(){
    System.out.println(data);
  }
  public String getSenderID() {
    return senderID;
  }

  public void setSenderID(String senderID) {
    this.senderID = senderID;
  }
  public String getChatID() {
    return chatID;
  }
  public void setChatID(String chatID) {
    this.chatID = chatID;
  }
  public Instant getTimestamp() {
    return timestamp;
  }
  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }
  public String getData() {
    return data;
  }
  public void setData(String data) {
    this.data = data;
  }
}
