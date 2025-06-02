package org.project.securechat.sharedClass;
import java.io.IOException;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Message{
  static public enum MessageTYPE{
    TEXT,
    FILE,
    KEY_EXCHANGE,
    COMMAND
  }
  public Message(){
    this.timestamp = Instant.now();
  }
  public Message(String senderID, String chatID,MessageTYPE messageType, String data){
    this.senderID = senderID;
    this.chatID = chatID;
    this.messageType = messageType;
    this.timestamp = Instant.now();
    this.data = data;
  }
  private String senderID;
  private String chatID;
  private Instant timestamp;
  @JsonProperty("messageType")
  private MessageTYPE messageType;
  private String data;

  static public String toJSON(Message message) {
    try {
        return JsonConverter.parseObjectToJson(message);
    } catch (IOException e) {
        throw new RuntimeException("Error serializing message to JSON", e);
    }
  }

  public void write(){
    System.out.println(data);
  }
  
}
