package nexuschat.service;

/**
 * Unit II Syllabus: Interfaces & Polymorphism
 * Defines contract for symmetric message encryption and decryption.
 */
public interface EncryptionService {

    /**
     * Encrypts plaintext into a ciphertext string.
     *
     * @param plainText original message content
     * @return encrypted string
     */
    String encrypt(String plainText);

    /**
     * Decrypts ciphertext back into the original plaintext.
     *
     * @param cipherText encrypted string
     * @return original plaintext
     */
    String decrypt(String cipherText);
}
