package nexuschat.exception;

/**
 * Unit III Syllabus:
 * - Creating own exception subclasses
 * - Thrown when a targeted recipient user cannot be resolved
 */
public class UserNotFoundException extends NexusException {
    private static final long serialVersionUID = 1L;

    public UserNotFoundException(String username) {
        super("User '" + username + "' was not found or is currently offline.");
    }
}
