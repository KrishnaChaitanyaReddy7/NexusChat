package nexuschat.exception;

/**
 * Unit III Syllabus:
 * - Creating own exception subclasses
 * - Thrown when network transport or socket connectivity fails
 */
public class NetworkException extends NexusException {
    private static final long serialVersionUID = 1L;

    public NetworkException(String message) {
        super(message);
    }

    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
