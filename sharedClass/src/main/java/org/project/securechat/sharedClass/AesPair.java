package org.project.securechat.sharedClass;

public class AesPair {
  

  private String sender;
  private String receiver;
    private String aesSender;
  private String aesReceiver;
  public AesPair(){}
  public String getAesSender() {
    return aesSender;
  }
  public void setAesSender(String aesSender) {
    this.aesSender = aesSender;
  }
  public String getAesReceiver() {
    return aesReceiver;
  }
  public void setAesReceiver(String aesReceiver) {
    this.aesReceiver = aesReceiver;
  }
  public String getSender() {
    return sender;
  }
  public void setSender(String sender) {
    this.sender = sender;
  }
  public String getReceiver() {
    return receiver;
  }
  public void setReceiver(String receiver) {
    this.receiver = receiver;
  }
  public AesPair(String aesSender, String aesReceiver, String sender, String receiver) {
    this.aesSender = aesSender;
    this.aesReceiver = aesReceiver;
    this.sender = sender;
    this.receiver = receiver;
  }
}
