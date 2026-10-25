package nexuschat.model;

import java.io.Serializable;

/**
 * Unit I Syllabus: Enum types, Constants
 */
public enum UserStatus implements Serializable {
    ONLINE("Online"),
    AWAY("Away"),
    BUSY("Do Not Disturb"),
    OFFLINE("Offline");

    private final String displayName;

    UserStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
