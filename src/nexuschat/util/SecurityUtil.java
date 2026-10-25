package nexuschat.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Unit I & IV Syllabus:
 * - String and Character operations
 * - Byte array manipulation and hex conversion
 * - Standard Java SE Security API (SHA-256)
 */
public final class SecurityUtil {

    private static final String SALT = "NexusChatSalt_2026_Secure";

    private SecurityUtil() {
        // Enforce non-instantiability
    }

    /**
     * Generates a salted SHA-256 hash formatted as a 64-character hexadecimal string.
     *
     * @param password raw password
     * @return 64-character hex hash string
     */
    public static String hashPassword(String password) {
        if (password == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String salted = password + ":" + SALT;
            byte[] hashBytes = digest.digest(salted.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            return Integer.toHexString(password.hashCode());
        }
    }

    /**
     * Verifies an input password against stored hash with fallback for plaintext legacy passwords.
     *
     * @param inputPassword plain password from user input
     * @param storedHash hash stored in database
     * @return true if password matches
     */
    public static boolean verifyPassword(String inputPassword, String storedHash) {
        if (inputPassword == null || storedHash == null) {
            return false;
        }
        String computedHash = hashPassword(inputPassword);
        return storedHash.equals(computedHash) || storedHash.equals(inputPassword);
    }
}
