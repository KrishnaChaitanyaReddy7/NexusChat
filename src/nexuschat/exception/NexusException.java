package nexuschat.exception;

/**
 * Unit III Syllabus:
 * - Exception Handling
 * - Exception hierarchy
 * - Creating own exception subclasses
 */
public class NexusException extends Exception {
    private static final long serialVersionUID = 1L;

    // Overloaded Constructor 1
    public NexusException(String message) {
        super(message);
    }

    // Overloaded Constructor 2 with causal exception
    public NexusException(String message, Throwable cause) {
        super(message, cause);
    }
}
