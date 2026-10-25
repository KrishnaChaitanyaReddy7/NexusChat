package nexuschat.exception;

/**
 * Unit III Syllabus:
 * - Creating own exception subclasses
 * - Thrown when user inputs fail business constraints (e.g., blank fields, too long)
 */
public class ChatValidationException extends NexusException {
    private static final long serialVersionUID = 1L;

    public ChatValidationException(String message) {
        super(message);
    }
}
