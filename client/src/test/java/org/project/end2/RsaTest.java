package org.project.end2;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.*;

import javax.crypto.BadPaddingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.project.end2.implementations.*;
import static org.junit.jupiter.api.Assertions.*;
import org.project.end2.Rsa;
/**
 * Unit test for simple App.
 */

public class RsaTest
{
private static final Logger LOGGER = LogManager.getLogger(); 
 Rsa rsa = new RsaImp();
  KeyPair keys = null;
  @BeforeEach
  void setUp() throws NoSuchAlgorithmException{
   
    keys = rsa.generatePairOfKeys();
    
    rsa.writeKeysToFile(keys);
  }

 
  @Test
  void rsaTestPublic(){
  
    PublicKey pub = rsa.readPubKeyFromFile();
    
    assertArrayEquals(keys.getPublic().getEncoded(),pub.getEncoded());
    
  }
  @Test
  void rsaTestPrivate(){

    PrivateKey priv = rsa.readPrivKeyFromFile();
     assertArrayEquals(keys.getPrivate().getEncoded(), priv.getEncoded());
  }

  @Test
  void encodeMessage() {
    String msg = " wiadomosc";
    String msgH = "X".repeat(120);
   
    byte[] msgE = rsa.encodeMessage(keys.getPublic(), msg.getBytes(StandardCharsets.UTF_8));
    byte[] msgDB = null;
    try{
      msgDB = rsa.decodeMessage(keys.getPrivate(),msgE);
    }
    catch(BadPaddingException e){}
    String msgD = rsa.byteToString(msgDB);
    assertEquals(msg, msgD);
    LOGGER.info("encode : msg {}  msgD {}",msg,msgD);
   
   
    }
    @Test
    void difEncodeKeys(){
      assertThrows(BadPaddingException.class,()->{

      
        String msg = "wiadomosc";
        KeyPair keysNew = rsa.generatePairOfKeys();

        byte[] msgE = rsa.encodeMessage(keys.getPublic(), msg.getBytes(StandardCharsets.UTF_8));
      
        byte[] msgDB = rsa.decodeMessage(keys.getPrivate(),msgE);
        byte[] msgDBWrong = rsa.decodeMessage(keysNew.getPrivate(), msgE);
        String msgD = rsa.byteToString(msgDB);
        String msgDWrong = rsa.byteToString(msgDBWrong);

      },"BadPaddingException expected");
      LOGGER.info("dif encode : Exception thrown");
      
    }
    @Test
    void speedOfRsa(){
       long startTime = System.nanoTime(); 
      
        encodeMessage();
        System.out.println("Zadanie zakończone.");
      
     
        long endTime = System.nanoTime(); 
       


        long durationNanos = endTime - startTime; // Oblicz różnicę w nanosekundach
        double durationMillis = (double) durationNanos / 1000000.0;
         LOGGER.info("speedOfRsa : Czas wykonania: " + String.format("%.3f", durationMillis) + " milisekund");
    }
    
}
