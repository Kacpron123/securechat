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
public class EncryptionService {
  
  private static Rsa rsa = new RsaImp();
  private static Aes aes = new AesImp();

  public static KeyPair generatePairOfRsaKeys(){
    return rsa.generatePairOfKeys();
  }
  public static void saveRsaKeysToFile(KeyPair rsaKeyPair){
    rsa.writeKeysToFile(rsaKeyPair);
  }
  public static PublicKey readPublicKeyFromFile(){
    return rsa.readPubKeyFromFile();
  }
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
   * 
   * @param privKey private rsa key
   * @param toDecode string64 message to decrypt
   * @return decrypted bytes 
   * @throws BadPaddingException throws if wrong key
   */
  public static byte[] decodeWithRsa(PrivateKey privKey,String toDecode)throws BadPaddingException{
    try{
      return rsa.decodeMessage(privKey, rsa.base64toBytes(toDecode));
    }catch(BadPaddingException e){
      throw new BadPaddingException("BAD KEY");
    } 
     
  }
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
  public static String getString64FromBytes(byte[] bytes){
    return aes.byteTo64String(bytes);
  }
  public static byte[] getBytesFromString64(String string64){
    return aes.base64toBytes(string64);
  }
  
  public static PublicKey getPublicKeyFromBytes(byte[] keyBytes) throws Exception {
      X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePublic(keySpec);
  }
  public static SecretKey getAesFromBytes(byte[] aesKey){
    return aes.getKeyFromBytes(aesKey);
  }
}
