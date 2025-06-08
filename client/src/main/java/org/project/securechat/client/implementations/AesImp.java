package org.project.securechat.client.implementations;

import org.project.securechat.client.Aes;
  
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
public class AesImp implements Aes {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_KEY_SIZE_BITS = 256; // Można zmienić na 128 lub 192
    private static final int GCM_IV_LENGTH_BYTES = 12; // Zalecane 12 bajtów (96 bitów) dla GCM
    private static final int GCM_TAG_LENGTH_BITS = 128; // Zalecane 128 bitów (16 bajtów) dla GCM

    private SecureRandom secureRandom;

     public AesImp() {
        this.secureRandom = new SecureRandom(); // Inicjalizacja
    }
    @Override
    public SecretKey generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGen.init(AES_KEY_SIZE_BITS, secureRandom);
            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            // W praktycznym systemie lepiej rzucić własny, bardziej specyficzny wyjątek
            throw new RuntimeException("Nie można wygenerować klucza AES: " + e.getMessage(), e);
        }
    }
    @Override
     public SecretKey getKeyFromBytes(byte[] keyBytes){
       if (keyBytes == null) {
            throw new IllegalArgumentException("Tablica bajtów klucza nie może być null.");
        }
        // Opcjonalnie: można dodać walidację długości klucza
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            System.err.println("Ostrzeżenie: Długość klucza (" + keyBytes.length +
                               " bajtów) nie jest standardową długością dla AES (16, 24, lub 32 bajty). " +
                               "Może to prowadzić do błędów podczas inicjalizacji Cipher.");
            // Można tu rzucić wyjątek, jeśli wymagamy ścisłej zgodności:
            // throw new IllegalArgumentException("Nieprawidłowa długość klucza AES: " + keyBytes.length + " bajtów.");
        }
        return new SecretKeySpec(keyBytes, AES_ALGORITHM);
    }

     
    @Override
    public GCMParameterSpec generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        return new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
    }

    @Override
    public byte[] encodeMessage(Key key, byte[] message) {
        if (!(key instanceof SecretKey)) {
            throw new IllegalArgumentException("Klucz musi być instancją SecretKey dla AES.");
        }
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            GCMParameterSpec gcmParameterSpec = generateIV(); // Generuj nowy IV dla każdego szyfrowania
            byte[] ivBytes = gcmParameterSpec.getIV();

            cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);
            byte[] cipherText = cipher.doFinal(message);

            // Połącz IV z zaszyfrowaną wiadomością: IV + ciphertext
            // To jest powszechna praktyka, aby strona deszyfrująca mogła odczytać IV.
            ByteBuffer byteBuffer = ByteBuffer.allocate(ivBytes.length + cipherText.length);
            byteBuffer.put(ivBytes);
            byteBuffer.put(cipherText);
            return byteBuffer.array();

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException("Błąd podczas szyfrowania: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] decodeMessage(Key key, byte[] encryptedMessageWithIv) throws BadPaddingException {
        if (!(key instanceof SecretKey)) {
            throw new IllegalArgumentException("Klucz musi być instancją SecretKey dla AES.");
        }
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedMessageWithIv);

            // Odczytaj IV z początku wiadomości
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byteBuffer.get(iv);

            // Reszta to zaszyfrowane dane
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);

            return cipher.doFinal(cipherText);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException | IllegalBlockSizeException e) {
            // BadPaddingException (lub AEADBadTagException, która jest jej podklasą w przypadku GCM)
            // jest rzucany przez doFinal, jeśli deszyfrowanie się nie powiedzie (np. zły klucz, uszkodzone dane).
            // Dlatego jest osobno w sygnaturze metody.
            throw new RuntimeException("Błąd podczas deszyfrowania: " + e.getMessage(), e);
        }
        // BadPaddingException (w tym AEADBadTagException) jest rzucany przez doFinal, jeśli tag się nie zgadza
    }

    @Override
    public String byteTo64String(byte[] message) {
        return Base64.getEncoder().encodeToString(message);
    }

    @Override
    public String base64ToString(String base64Encoded) {
        byte[] decodedBytes = Base64.getDecoder().decode(base64Encoded);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    @Override
    public byte[] base64toBytes(String base64Encoded) {
        return Base64.getDecoder().decode(base64Encoded);
    }

   
}

