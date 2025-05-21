package org.project.securechat.client;
import java.security.*;
import org.junit.jupiter.api.*;
import org.project.securechat.client.implementations.*;
import static org.junit.jupiter.api.Assertions.*;
import org.project.securechat.client.Client;
import org.project.securechat.sharedClass.Message;;
public class ClientTest
{
 
  @Test
  void clientOn(){
    Client.main(null);
    Message message = new Message("hello im under the water");
    
    message.write();
  }
  
}
