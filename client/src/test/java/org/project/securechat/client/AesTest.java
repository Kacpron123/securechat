package org.project.securechat.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.project.securechat.client.implementations.AesImp;
import org.project.securechat.client.implementations.RsaImp;

import java.security.KeyPair;
public class AesTest {
  


  String message = "Ciało prążkowane i gałka blada odpowiadają za tempo mówienia.\r\n";
        
  Aes aesGcm = new AesImp();
  @BeforeEach
  public void setUp(){

  }
  @Test
  void encodeMessage(){
    SecretKey secretKey = aesGcm.generateKey();
     byte[] encryptedBytesWithIv = aesGcm.encodeMessage(secretKey, message.getBytes(StandardCharsets.UTF_8));
    String messageEn64 = aesGcm.byteTo64String(encryptedBytesWithIv);
     System.out.println("Zaszyfrowana wiadomość (Base64, zawiera IV): " + messageEn64);


  }
  @Test
  void decodeMessage(){
     SecretKey secretKey = aesGcm.generateKey();
     byte[] encryptedBytesWithIv = aesGcm.encodeMessage(secretKey, message.getBytes(StandardCharsets.UTF_8));
    String messageEn64 = aesGcm.byteTo64String(encryptedBytesWithIv);
     System.out.println("Zaszyfrowana wiadomość (Base64, zawiera IV): " + messageEn64);
// 3. Deszyfrowanie wiadomości
        try {
            // Symulacja otrzymania zaszyfrowanej wiadomości (np. z sieci)
            byte[] receivedEncryptedBytesWithIv = aesGcm.base64toBytes(messageEn64);
            
            byte[] decryptedBytes = aesGcm.decodeMessage(secretKey, receivedEncryptedBytesWithIv);
            String decryptedMessage = new String(decryptedBytes, StandardCharsets.UTF_8);
            System.out.println("Odszyfrowana wiadomość: " + decryptedMessage);
            assertEquals(message, decryptedMessage);
          
        } catch (BadPaddingException e) {
            System.err.println("Błąd deszyfrowania (np. zły klucz, uszkodzone dane, zły tag): " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Inny błąd podczas deszyfrowania: " + e.getMessage());
            e.printStackTrace();
        }
  }
  @Test
  void rsa_aes_encryption(){
    SecretKey secretKey = aesGcm.generateKey();
    Rsa rsa = new RsaImp();
    KeyPair keyPair = rsa.generatePairOfKeys();
    byte[] encodedKey = rsa.encodeMessage(keyPair.getPublic(), secretKey.getEncoded());
    String encoded64Key = rsa.byteTo64String(encodedKey);
    System.out.println(encoded64Key);
     byte[] decodedKey = null;
    try{
     decodedKey = rsa.decodeMessage(keyPair.getPrivate(), rsa.base64toBytes(encoded64Key));
    }
    catch(BadPaddingException e){
      System.out.println(e);
    }
    assertArrayEquals(secretKey.getEncoded(),decodedKey);

  }
  @Test
  void rsa_aes_message_encryption(){
    SecretKey secretKey = aesGcm.generateKey();
    Rsa rsa = new RsaImp();
    String encodedAesMessage64 = aesGcm.byteTo64String(aesGcm.encodeMessage(secretKey, message.getBytes()));
    
    KeyPair keyPair = rsa.generatePairOfKeys();
    byte[] encodedKey = rsa.encodeMessage(keyPair.getPublic(), secretKey.getEncoded());
    String encoded64Key = rsa.byteTo64String(encodedKey);
     byte[] decodedKey = null;
    try{
     decodedKey = rsa.decodeMessage(keyPair.getPrivate(), rsa.base64toBytes(encoded64Key));
    }
    catch(BadPaddingException e){
      System.out.println(e);
    }
    SecretKey decodedSecretKey = aesGcm.getKeyFromBytes(decodedKey);
    byte [] tmp = rsa.base64toBytes(encodedAesMessage64);
    try{
       String decodedMessage = new String(aesGcm.decodeMessage(decodedSecretKey,tmp));

    System.out.println(decodedMessage);

    assertEquals(message, decodedMessage);
    }catch(BadPaddingException e){
      System.out.println(e);
    }
   
  }
}
