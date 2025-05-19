package org.project.end2;
import java.io.File;
import java.security.*;
import org.junit.jupiter.api.*;
import org.project.end2.implementations.*;
import static org.junit.jupiter.api.Assertions.*;
import org.project.end2.Rsa;
/**
 * Unit test for simple App.
 */

public class RsaTest
{
 
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
  void encodeMessage(){
    String msg = " wiadomosc";
    String msgE = rsa.encodeMessage(keys.getPublic(), msg);
   
    String msgD = rsa.decodeMessage(keys.getPrivate(),msgE);
    assertEquals(msg, msgD);
    }
}
