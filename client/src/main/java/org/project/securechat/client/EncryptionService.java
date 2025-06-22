package org.project.securechat.client;

import org.project.securechat.client.implementations.AesImp;
import org.project.securechat.client.implementations.RsaImp;

import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.SecretKey;
import javax.crypto.BadPaddingException;

import java.security.PrivateKey;

/**
 * This class provides static methods for encrypting and decrypting messages using AES and RSA.
 * 
 * 
 */
public class EncryptionService {
  private static Rsa rsa = new RsaImp();
  private static Aes aes = new AesImp();


  // creating rsa keys if not exist
  static{
    // TODO multiple users rsa
    PublicKey public_key=rsa.readPubKeyFromFile();
    if(public_key==null)
      rsa.generatePairOfKeys();
  }


  /**
   * Generates a pair of RSA keys.
   * 
   * @return A new pair of RSA keys.
   */
  public static KeyPair generatePairOfRsaKeys(){
    return rsa.generatePairOfKeys();
  }
  /**
   * Saves the given RSA key pair to a file.
   * 
   * @param rsaKeyPair The key pair to save.
   */
  public static void saveRsaKeysToFile(KeyPair rsaKeyPair){
    rsa.writeKeysToFile(rsaKeyPair);
  }
  /**
   * Returns the public RSA key that is used for encryption.
   * 
   * @return The public RSA key.
   */
  public static PublicKey readPublicKeyFromFile(){
    return rsa.readPubKeyFromFile();
  }
  /**
   * Reads the private RSA key from a file.
   * 
   * @return The private RSA key.
   */
  public static PrivateKey readPrivateKeyFromFile(){
    return rsa.readPrivKeyFromFile();
  }
  /**
   * 
   * @param pubKey public rsa key
   * @param toEncode bytes to encode
   * @return return String64 
   */
  public static String encodeWithRsa(PublicKey pubKey,byte[] toEncode){
    return rsa.byteTo64String(rsa.encodeMessage(pubKey, toEncode));
  }
  /**
   * decode message
   * 
   * @param privKey private rsa key
   * @param toDecode string64 message to decrypt
   * @return decrypted bytes 
   * @throws BadPaddingException throws if wrong key
   */
  public static byte[] decodeWithRsa(PrivateKey privKey,String toDecode) throws BadPaddingException{
    try{
      return rsa.decodeMessage(privKey, rsa.base64toBytes(toDecode));
    }catch(BadPaddingException e){
      throw new BadPaddingException("BAD KEY");
    } 
     
  }
  /**
   * Generates a new AES key.
   * 
   * @return A new AES key.
   */
  public static SecretKey createAesKey(){
    return aes.generateKey();
  }
  /**
   * 
   * @param aes64String string64 aes key
   * @return return aes key
   */
  public static SecretKey getAesKeyFromString(String aes64String){
    return aes.getKeyFromBytes(aes.base64toBytes(aes64String));
  }
  /**
   * 
   * @param aesKey aes key
   * @param toEncrypt string too encrypt
   * @return return encrypted string 64
   */
  public static String encryptWithAesKey(SecretKey aesKey,String toEncrypt){
    return aes.byteTo64String(aes.encodeMessage(aesKey, toEncrypt.getBytes(Charset.defaultCharset())));
  }
  /**
   * 
   * @param aesKey aes key
   * @param toDecrypt string64 to decrypt
   * @return returns decrypted normal String
   */
  public static String decryptWithAesKey(SecretKey aesKey,String toDecrypt) throws BadPaddingException{
    try{
      return new String(aes.decodeMessage(aesKey, aes.base64toBytes(toDecrypt)),Charset.defaultCharset());
    }catch(BadPaddingException e){
      throw new BadPaddingException("WRONG KEY");
    }
    
  }
  /**
   * Converts a byte array into a Base64 encoded string.
   * 
   * @param bytes the byte array to be converted
   * @return a Base64 encoded string representation of the byte array
   */
  public static String getString64FromBytes(byte[] bytes){
    return aes.byteTo64String(bytes);
  }
  /**
   * Converts a Base64 encoded string into a byte array.
   * 
   * @param string64 the Base64 encoded string to be converted
   * @return a byte array representation of the Base64 encoded string
   */
  public static byte[] getBytesFromString64(String string64){
    return aes.base64toBytes(string64);
  }
  
  /**
   * Converts a byte array containing a public RSA key into a PublicKey object.
   * 
   * @param keyBytes the byte array containing the public RSA key
   * @return a PublicKey object representing the public RSA key
   * @throws Exception if the given byte array does not contain a valid public RSA key
   */
  public static PublicKey getPublicKeyFromBytes(byte[] keyBytes) throws Exception {
      X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePublic(keySpec);
  }
  /**
   * Converts a byte array containing a secret AES key into a SecretKey object.
   * 
   * @param aesKey the byte array containing the secret AES key
   * @return a SecretKey object representing the secret AES key
   */
  public static SecretKey getAesFromBytes(byte[] aesKey){
    return aes.getKeyFromBytes(aesKey);
  }
}
