package org.project.securechat.sharedClass;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

public class Message {
  public Message() {
  }
  
  public Message(long senderID, long chatID, DataType dataType, String data) {
    this(senderID, chatID, dataType, data, Instant.now().toString());
  }
    public Message(long senderID, long chatID, DataType dataType, String data,String timestamp) {
    this.senderID = senderID;
    this.chatID = chatID;
  
    this.dataType = dataType;
    this.timestamp = Instant.parse(timestamp);
    
    this.data = data;
  
  }


  private long senderID;
  private long chatID;
  private Instant timestamp;
  private DataType dataType;
  private String data; // for now




  public DataType getDataType() {
    return dataType;
  }

  public void setDataType(DataType dataType) {
    this.dataType = dataType;
  }

  // public byte[] message;
 
  public enum DataType {
    TEXT,
    FILE, //for now not used
    KEY_EXCHANGE,
    RSA_KEY,
    CONFIRMATION, //any kind of confirmation
    ABORT, //any kind of not confirmation
    CREATE_2_CHAT, //create 2 person chat chat
    CREATE_G_CHAT, //create multiple person chat
    CLOSE_CONNECTION;
  }

  public void write() {
    System.out.println(data);
  }

  public long getSenderID() {
    return senderID;
  }

  public void setSenderID(long senderID) {
    this.senderID = senderID;
  }

  public long getChatID() {
    return chatID;
  }

  public void setChatID(long chatID) {
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
