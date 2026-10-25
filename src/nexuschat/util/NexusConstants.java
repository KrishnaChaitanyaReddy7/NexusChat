package nexuschat.util;

/**
 * Unit II Syllabus: Using the final keyword.
 * - A 'final' class cannot be subclassed (preventing inheritance).
 * - 'final' variables act as constants whose values cannot be modified once initialized.
 * - Private constructor prevents instantiation.
 */
public final class NexusConstants {

    // Final constant variables (Unit I: Data types & Unit II: Final keyword)
    public static final String APP_NAME = "NexusChat";
    public static final String APP_VERSION = "1.0.0";
    public static final int DEFAULT_PORT = 8888;
    public static final String DEFAULT_HOST = "localhost";
    public static final String DB_URL = "jdbc:sqlite:nexuschat.db";
    public static final String DB_DRIVER = "org.sqlite.JDBC";
    
    // Limits
    public static final int MAX_USERNAME_LENGTH = 20;
    public static final int MIN_PASSWORD_LENGTH = 4;
    public static final int MAX_MESSAGE_LENGTH = 1000;

    // Private constructor prevents instantiation
    private NexusConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
