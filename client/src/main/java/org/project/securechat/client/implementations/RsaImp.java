package org.project.securechat.client.implementations;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.crypto.*;

import org.project.securechat.client.Rsa;
public class RsaImp implements Rsa{

  private static final Logger LOGGER = LogManager.getLogger(); 

  public KeyPair generatePairOfKeys(){
    try{
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      
      return generator.generateKeyPair();
      
    }catch(NoSuchAlgorithmException e){
      LOGGER.error("generatePairOfKeys ERROR NO_ALGORITHM",e);
      
    }
    return null;
    
  }
   public String byteTo64String(byte[] message) {
    // Zamienia bajty na Base64 tekst
    return Base64.getEncoder().encodeToString(message);
}
public String base64ToString(String base64Encoded) {
    byte[] decodedBytes = Base64.getDecoder().decode(base64Encoded);
    return new String(decodedBytes, StandardCharsets.UTF_8);
}
public byte[] base64toBytes(String base64Encoded){
  return Base64.getDecoder().decode(base64Encoded);
}

  public byte[] encodeMessage( Key key,byte[] message) {
    try{
      Cipher encryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      encryptCipher.init(Cipher.ENCRYPT_MODE, key);

     byte[] bytes = encryptCipher.doFinal(message);
    return bytes;

    }catch(NoSuchAlgorithmException e){
      LOGGER.error("encodeMessage :No algorithm found",e);
    }
    catch(NoSuchPaddingException  | BadPaddingException e){
      LOGGER.error("encodeMessage : NoSuchPaddingException",e);
    }
    catch(InvalidKeyException  e){
      LOGGER.error("encodeMessage : InvalidKeyException",e);
    }catch(IllegalBlockSizeException e){
      LOGGER.error("encodeMessage : IllegalBlockingSizeException",e);
    }
    return null;
   
  }
  public byte[] decodeMessage(Key key,byte[] message) throws BadPaddingException{
    
    try{
      
      Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      cipher.init(Cipher.DECRYPT_MODE, key);
      
      byte[] bytes = cipher.doFinal(message);
      return bytes;
    }catch(NoSuchAlgorithmException e){
      LOGGER.error("decodeMessage :No algorithm found",e);
    }
    catch(NoSuchPaddingException  | BadPaddingException e){
      LOGGER.error("decodeMessage : NoSuchPaddingException",e);
      throw new BadPaddingException("use of wrong key");
    }
    catch(InvalidKeyException e){
      LOGGER.error("decodeMessage : InvalidKeyException",e);
    }
    catch(IllegalBlockSizeException e){
      LOGGER.error("decodeMessage : IllegalBlockingSizeException",e);
    }

    return null;
  }
  static public boolean checkIfKeysCreated() {
    File publicKeyFile = new File("key.pub");
    File privateKeyFile = new File("key.priv");
    return publicKeyFile.exists() && privateKeyFile.exists();
  }
  // TODO static inicjalization, cleaner code RSAImp
  public void writeKeysToFile(KeyPair keyPair) {
    try {
      String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
      String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

      Files.write(new File("key.pub").toPath(), publicKeyBase64.getBytes(StandardCharsets.UTF_8));
      Files.write(new File("key.priv").toPath(), privateKeyBase64.getBytes(StandardCharsets.UTF_8));

      LOGGER.info("writeKeysToFile: keys saved in Base64 format");
    } catch (IOException e) {
      LOGGER.error("writeKeysToFile: IO error", e);
    }
  }
 public PublicKey readPubKeyFromFile() {
    try {
      LOGGER.warn("readPubKey: loading Base64 key");
      byte[] base64Bytes = Files.readAllBytes(new File("key.pub").toPath());
      byte[] keyBytes = Base64.getDecoder().decode(base64Bytes);

      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      LOGGER.error("readPubKey: Failed to load key");
    }
    return null;
  }

  public PrivateKey readPrivKeyFromFile() {
    try {
      LOGGER.warn("readPrivKey: loading Base64 key");
      byte[] base64Bytes = Files.readAllBytes(new File("key.priv").toPath());
      byte[] keyBytes = Base64.getDecoder().decode(base64Bytes);

      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      LOGGER.error("readPrivKey: Failed to load key");
    }
    return null;
  }
}


