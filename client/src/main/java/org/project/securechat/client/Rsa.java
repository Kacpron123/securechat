package org.project.securechat.client;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;

import java.security.Key;

public interface Rsa {
  public KeyPair generatePairOfKeys();

  public void writeKeysToFile(KeyPair keyPair);

  public PublicKey readPubKeyFromFile();

  public PrivateKey readPrivKeyFromFile();

  public byte[] encodeMessage(Key key, byte[] message);

  public byte[] decodeMessage(Key key, byte[] message) throws BadPaddingException;
 /**
   * Changes bytes into 64 string
   * 
   * @param message message in bytes (after encoding).
   *
   */
  public String byteTo64String(byte[] message);
  /**
   *Changes base64 to normal string
   * 
   * @param base64Encoded message in 64base string
   * 
   */
  public String base64ToString(String base64Encoded);
  /**
   * Changes base64 string to bytes
   * 
   * @param base64Encoded base64 string to bytes
   * 
   */
  public byte[] base64toBytes(String base64Encoded);
}
