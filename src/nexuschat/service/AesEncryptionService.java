package nexuschat.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Standard Java SE symmetric encryption using AES (Advanced Encryption Standard).
 * Uses standard javax.crypto and java.security APIs without external dependencies.
 */
public class AesEncryptionService implements EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String PREFIX = "ENC:";
    private final SecretKeySpec secretKey;

    public AesEncryptionService() {
        this("NexusChatEncryptionKey2026");
    }

    public AesEncryptionService(String secretPassphrase) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(secretPassphrase.getBytes(StandardCharsets.UTF_8));
            byte[] aesKey = new byte[16]; // 128-bit key
            System.arraycopy(keyBytes, 0, aesKey, 0, 16);
            this.secretKey = new SecretKeySpec(aesKey, ALGORITHM);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to initialize AES key: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        if (plainText.startsWith(PREFIX)) {
            return plainText;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception ex) {
            return plainText;
        }
    }

    @Override
    public String decrypt(String cipherText) {
        if (cipherText == null || !cipherText.startsWith(PREFIX)) {
            return cipherText;
        }
        try {
            String base64Payload = cipherText.substring(PREFIX.length());
            byte[] decodedBytes = Base64.getDecoder().decode(base64Payload);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return cipherText;
        }
    }
}
