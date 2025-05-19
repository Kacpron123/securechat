package org.project.end2.implementations;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.crypto.*;

import org.project.end2.Rsa;
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
  public String byteToString(byte[] message){
    return new String(message,StandardCharsets.UTF_8);
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
  public byte[] decodeMessage(Key key,byte[] message){
    
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
    }
    catch(InvalidKeyException e){
      LOGGER.error("decodeMessage : InvalidKeyException",e);
    }
    catch(IllegalBlockSizeException e){
      LOGGER.error("decodeMessage : IllegalBlockingSizeException",e);
    }

    return null;
  }
  public void writeKeysToFile(KeyPair keyPair){
      
      PrivateKey privateKey = keyPair.getPrivate();
      PublicKey publicKey = keyPair.getPublic();
      LOGGER.info("writeKeysToFile ");
        try {
            FileOutputStream pub = new FileOutputStream("key.pub");
            pub.write(publicKey.getEncoded());
            pub.close();

           
            FileOutputStream priv = new FileOutputStream("key.priv");
            priv.write(privateKey.getEncoded()); 
            priv.close();

        }

    catch(FileNotFoundException e){
      System.out.println("File not found");
    }
    catch(IOException e){
      System.out.println("IO exception");
    }
   
  }
  public PublicKey readPubKeyFromFile(){
    
    try{
      LOGGER.warn("readPubKey : File might exists");
      File publicKeyFile = new File("key.pub");
      
      byte[] publicKeyBytes = Files.readAllBytes(publicKeyFile.toPath());

      KeyFactory publicKeyFactory = KeyFactory.getInstance("RSA");
      EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
      PublicKey publicKey = publicKeyFactory.generatePublic(publicKeySpec);
    return publicKey; 
    }
    
    catch(IOException e){
      System.out.println("Io except");
    }
    catch(NoSuchAlgorithmException e){
       System.out.println("Algorithm not found");
    }
    catch(InvalidKeySpecException e){
      System.out.println("InvalidKeySpec");
    }
    return null;
  }
  public  PrivateKey readPrivKeyFromFile(){
    
    try{
      LOGGER.warn("readPrivKey : File might exists");
     File privateKeyFile = new File("key.priv");
    byte[] privateKeyBytes = Files.readAllBytes(privateKeyFile.toPath());
    
    KeyFactory privateKeyFactory = KeyFactory.getInstance("RSA");
    EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
      return privateKeyFactory.generatePrivate(privateKeySpec);
    }
    catch(FileNotFoundException e){
      System.out.println("private key not found");
    }
    catch(IOException e){
      System.out.println("Io except");
    }
    catch(NoSuchAlgorithmException e){
       System.out.println("Algorithm not found");
    }
    catch(InvalidKeySpecException e){
       System.out.println("InvalidKeySpec");
    }
    
    return null;
  }


}
