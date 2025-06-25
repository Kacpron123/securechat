package org.project.securechat.client;

import java.security.Key;

import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Interface for AES encryption and decryption.
 */
public interface Aes {

  /**
   * Generates a new AES key.
   * 
   * @return A new AES key.
   */
  public SecretKey generateKey();

  /**
   * Generates a new initialization vector (IV) for AES encryption.
   * 
   * @return A new IV.
   */
  public GCMParameterSpec generateIV();

  /**
   * Converts a byte array into an AES key.
   * 
   * @param keyInBytes The byte array to convert.
   * @return The AES key.
   */
  public SecretKey getKeyFromBytes(byte[] keyInBytes);

  /**
   * Encrypts a message using AES.
   * 
   * @param key The AES key to use for encryption.
   * @param message The message to encrypt.
   * @return The encrypted message.
   */
  public byte[] encodeMessage(Key key, byte[] message);

  /**
   * Decrypts a message using AES.
   * 
   * @param key The AES key to use for decryption.
   * @param message The message to decrypt.
   * @return The decrypted message.
   * @throws BadPaddingException If the decryption fails.
   */
  public byte[] decodeMessage(Key key, byte[] message) throws BadPaddingException;

  /**
   * Converts a byte array into a Base64 encoded string.
   * 
   * @param message The byte array to convert.
   * @return The Base64 encoded string.
   */
  public String byteTo64String(byte[] message);

  /**
   * Converts a Base64 encoded string into a normal string.
   * 
   * @param base64Encoded The Base64 encoded string to convert.
   * @return The normal string.
   */
  public String base64ToString(String base64Encoded);

  /**
   * Converts a Base64 encoded string into a byte array.
   * 
   * @param base64Encoded The Base64 encoded string to convert.
   * @return The byte array.
   */
  public byte[] base64toBytes(String base64Encoded);

}
