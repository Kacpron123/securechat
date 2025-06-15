package org.project.securechat.sharedClass;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

public class Message {
  public Message() {
  }

  public Message(String senderID, String chatID, DataType dataType, String data) {
    this.senderID = senderID;
    this.chatID = chatID;
  
    this.dataType = dataType;
    this.timestamp = Instant.now();
    
    this.data = data;
  
  }
    public Message(String senderID, String chatID, DataType dataType, String data,String timestamp) {
    this.senderID = senderID;
    this.chatID = chatID;
  
    this.dataType = dataType;
    this.timestamp = Instant.parse(timestamp);
    
    this.data = data;
  
  }

  private String senderID;
  private String chatID;
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
    AES_KEY,
    AES_EXCHANGE,
    CONFIRMATION, //any kind of confirmation
    ABORT, //any kind of not confirmation
    CREATECHAT,
    CREATEGROUPCHAT,
    CLOSE_CONNECTION;
  }

  public void write() {
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
