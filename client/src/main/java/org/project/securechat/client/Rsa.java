package org.project.securechat.client;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;

import java.security.Key;

public interface Rsa {
  /**
   * Generates pair of keys
   * @return key pair
   */
  public KeyPair generatePairOfKeys();
  /**
   * Writes keys to file
   * @param keyPair key pair
   */
  public void writeKeysToFile(KeyPair keyPair);
  /**
   * Reads public key from file
   * @return public key
   */
  public PublicKey readPubKeyFromFile();
  /**
   * Reads private key from file
   * @return private key
   */
  public PrivateKey readPrivKeyFromFile();
  /**
   * Encodes message
   * @param key rsa key
   * @param message message
   * @return message in bytes
   */
  public byte[] encodeMessage(Key key, byte[] message);
  /**
   * Decodes message
   * @param key rsa key
   * @param message message
   * @return message in bytes
   * @throws BadPaddingException
   */
  public byte[] decodeMessage(Key key, byte[] message) throws BadPaddingException;
 /**
   * Changes bytes into 64 string
   * 
   * @param message message in bytes (after encoding).
   * @return message in 64base
   */
  public String byteTo64String(byte[] message);
  /**
   *Changes base64 to normal string
   * 
   * @param base64Encoded message in 64base string
   * @return message in normal string
   */
  public String base64ToString(String base64Encoded);
  /**
   * Changes base64 string to bytes
   * 
   * @param base64Encoded base64 string to bytes
   * @return message in bytes
   */
  public byte[] base64toBytes(String base64Encoded);
}
