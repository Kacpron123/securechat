package org.project.securechat.client;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.crypto.SecretKey;
import javax.crypto.BadPaddingException;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class EncryptionServiceTest {
  
  String message;
  @BeforeEach
  void setUp(){
    message = "witam tu tomek";
  }
  @Disabled
  @Test
  void testCreatingKeysAndSaveToFile(){
    EncryptionService.saveRsaKeysToFile(EncryptionService.generatePairOfRsaKeys());
    File pubKey = new File("key.pub");
    File privKey = new File("key.priv");
    assertAll(
      ()-> assertTrue(pubKey.exists()),
      ()-> assertTrue(privKey.exists())
    );

  }
  @Test
  void encryptRsaMessageDecodeMessage(){
    PublicKey pubKey = EncryptionService.readPublicKeyFromFile();
    String encodedStr = EncryptionService.encodeWithRsa(pubKey, message.getBytes());
    PrivateKey privKey = EncryptionService.readPrivateKeyFromFile();
    try{
      String decodedStr = new String(EncryptionService.decodeWithRsa(privKey, encodedStr));
      assertEquals(message, decodedStr);
      System.out.println(decodedStr);
    }catch(BadPaddingException e){
      System.out.println("WRONG KEY");
    }
    
  }
  @Test
  void encryptAesMessageDecodeMessage(){
    SecretKey aesKey = EncryptionService.createAesKey();
    String encodedStr = EncryptionService.encryptWithAesKey(aesKey, message);
    try{
      String decodedStr = EncryptionService.decryptWithAesKey(aesKey, encodedStr);
      assertEquals(message, decodedStr);
    }catch(BadPaddingException e){
      System.out.println("WRONG KEY");
    }
  }
    @Test
  void decryptRsaMessageWithWrongPrivateKey_shouldFail() {
    // Generujemy poprawną parę kluczy
    KeyPair correctKeys = EncryptionService.generatePairOfRsaKeys();
    String encodedStr = EncryptionService.encodeWithRsa(correctKeys.getPublic(), message.getBytes());

    // Generujemy inną parę kluczy (niepoprawny prywatny klucz)
    KeyPair wrongKeys = EncryptionService.generatePairOfRsaKeys();
    PrivateKey wrongPrivKey = wrongKeys.getPrivate();

    assertThrows(BadPaddingException.class, () -> {
      EncryptionService.decodeWithRsa(wrongPrivKey, encodedStr);
    });
  }
  @Test
  void decryptAesMessageWithWrongKey_shouldFail() {
    // Poprawny klucz AES
    SecretKey correctKey = EncryptionService.createAesKey();
    String encodedStr = EncryptionService.encryptWithAesKey(correctKey, message);

    // Niepoprawny klucz AES
    SecretKey wrongKey = EncryptionService.createAesKey(); // inny losowy klucz

    assertThrows(BadPaddingException.class, () -> {
      EncryptionService.decryptWithAesKey(wrongKey, encodedStr);
    });
  }
  @Test
  void finalTestRsaPlusAes(){
    PublicKey pubKey = EncryptionService.readPublicKeyFromFile();
    SecretKey secretKey = EncryptionService.createAesKey();
    String enMess = EncryptionService.encryptWithAesKey(secretKey, message);
    String enAesKey = EncryptionService.encodeWithRsa(pubKey, secretKey.getEncoded());
        byte []tmp= null;
    try{
      tmp =  EncryptionService.decodeWithRsa(EncryptionService.readPrivateKeyFromFile(), enAesKey);
    }catch(BadPaddingException e){
      System.out.println("WRONG KEY");
    }
 
    SecretKey deAesKey = EncryptionService.getAesKeyFromString(EncryptionService.getString64FromBytes(tmp));
    assertEquals(secretKey, deAesKey);
    try{
        String decodedMess = EncryptionService.decryptWithAesKey(deAesKey, enMess);
        assertEquals(message,decodedMess);
    }catch(BadPaddingException e){
      System.out.println("WRONG KEY");
    }
  
  }
  
@Test
void base64EncodingDecodingShouldBeReversible() {
  byte[] original = message.getBytes(StandardCharsets.UTF_8);
  String base64 = EncryptionService.getString64FromBytes(original);
  byte[] decoded = EncryptionService.getBytesFromString64(base64);
  assertArrayEquals(original, decoded);
}

}
