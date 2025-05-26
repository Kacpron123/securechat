package org.project.securechat.sharedClass;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

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
    mess = new Message("Adam","Pawel","bardzo wazna wiadomosc");
    System.out.println(mess.getTimestamp().toString());
    payload = "{ \"senderID\" : \"Pawel\", \"chatID\" : \"Adam\",\"timestamp\":\"2025-05-26T08:45:48.641588900Z\", \"data\":\"witam\" }";
  
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
  

