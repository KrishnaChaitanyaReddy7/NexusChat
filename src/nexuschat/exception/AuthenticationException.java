package nexuschat.exception;

/**
 * Unit III Syllabus:
 * - Creating own exception subclasses
 * - Thrown when authentication or login validation fails
 */
public class AuthenticationException extends NexusException {
    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
