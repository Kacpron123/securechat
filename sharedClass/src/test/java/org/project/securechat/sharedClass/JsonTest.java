package org.project.securechat.sharedClass;

import org.junit.jupiter.api.*;
import org.project.securechat.sharedClass.Message.DataType;

import java.io.IOException;
/**
 * Unit test for simple App.
 */

public class JsonTest
{
 
  Message mess;
  Message mess2;
  String payload;
  @BeforeEach
  void setUp(){
    mess = new Message(2,4,DataType.TEXT,"bardzo wazna wiadomosc");
    System.out.println(mess.getTimestamp().toString());
    payload = "{ \"senderID\" : 2, \"chatID\" : 4,\"timestamp\":\"2025-05-26T08:45:48.641588900Z\",\"dataType\":\"FILE\", \"data\":\"witam\" }";
  
  }
  @Test
  void payloadToObject(){
    Message tmp = null;
    try{
      tmp = JsonConverter.parseDataToObject(payload, Message.class);

    }catch(IOException e){
      System.out.println(e);
    }
    try{
      System.out.println(JsonConverter.parseObjectToJson(tmp));
      System.out.println(tmp.getDataType());
    }catch(Exception e){
      System.out.println(e);
    }
  }
    @Test
    void ObjectToPayload(){
      try{
        String payload1 = JsonConverter.parseObjectToJson(mess);
      System.out.println(payload1);
      }
      catch(Exception e){}
    }
     
   
    
  }
  

