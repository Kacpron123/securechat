package org.project.end2;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Key;
public interface Rsa {
  public   KeyPair generatePairOfKeys();
  public void writeKeysToFile(KeyPair keyPair);
  public  PublicKey readPubKeyFromFile();
  public PrivateKey readPrivKeyFromFile();
  public String encodeMessage( Key key,String message);
  public String decodeMessage(Key key,String message);
}
