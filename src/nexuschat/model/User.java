package nexuschat.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Unit I Syllabus:
 * - Concepts of classes, objects, constructors, methods, access control
 * - 'this' keyword and constructor chaining
 * - Method and constructor overloading
 * - Nested and inner classes (Static Nested Builder)
 * - Exploring String class & Type Wrappers
 * 
 * Unit II Syllabus:
 * - The Object class: overriding equals(), hashCode(), and toString()
 * 
 * Unit IV Syllabus:
 * - java.io.Serializable for network and persistent transmission
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    // Unit I: Member variables with private access control (Encapsulation)
    private int userId;                   // Type wrapper autoboxing/unboxing used in Dao
    private String username;
    private String fullName;
    private String passwordHash;
    private UserStatus status;
    private long registrationTimestamp;

    // Unit I: Constructor Overloading (Constructor 1 - Default minimal)
    public User() {
        this("guest_" + System.currentTimeMillis(), "Guest User");
    }

    // Unit I: Constructor Overloading (Constructor 2 - Username only)
    public User(String username) {
        this(username, username);
    }

    // Unit I: Constructor Overloading (Constructor 3 - Username and Full Name)
    public User(String username, String fullName) {
        // Constructor chaining using 'this' keyword
        this(0, username, fullName, "", UserStatus.OFFLINE, System.currentTimeMillis());
    }

    // Unit I: Constructor Overloading (Constructor 4 - Complete)
    public User(int userId, String username, String fullName, String passwordHash, UserStatus status, long registrationTimestamp) {
        // 'this' keyword disambiguates instance variables from parameters
        this.userId = userId;
        this.username = (username != null) ? username.trim() : "";
        this.fullName = (fullName != null) ? fullName.trim() : "";
        this.passwordHash = passwordHash;
        this.status = (status != null) ? status : UserStatus.OFFLINE;
        this.registrationTimestamp = registrationTimestamp;
    }

    // Unit I: Method Overloading (Form 1: update fullName only)
    public void updateProfile(String newFullName) {
        if (newFullName != null && !newFullName.trim().isEmpty()) {
            this.fullName = newFullName.trim();
        }
    }

    // Unit I: Method Overloading (Form 2: update fullName and status)
    public void updateProfile(String newFullName, UserStatus newStatus) {
        this.updateProfile(newFullName);
        if (newStatus != null) {
            this.status = newStatus;
        }
    }

    // Getters and Setters demonstrating Access Control
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = (username != null) ? username.trim() : "";
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public long getRegistrationTimestamp() {
        return registrationTimestamp;
    }

    public void setRegistrationTimestamp(long registrationTimestamp) {
        this.registrationTimestamp = registrationTimestamp;
    }

    // Unit II: Overriding methods inherited from java.lang.Object
    @Override
    public String toString() {
        return username + " (" + fullName + ") [" + status.getDisplayName() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o; // Type casting
        return Objects.equals(username.toLowerCase(), user.username.toLowerCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(username.toLowerCase());
    }

    // Unit I: Nested and Inner Classes - Static Nested Builder Pattern
    public static class Builder {
        private int userId;
        private String username;
        private String fullName;
        private String passwordHash;
        private UserStatus status = UserStatus.ONLINE;
        private long registrationTimestamp = System.currentTimeMillis();

        public Builder setUserId(int userId) {
            this.userId = userId;
            return this;
        }

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setFullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder setStatus(UserStatus status) {
            this.status = status;
            return this;
        }

        public Builder setRegistrationTimestamp(long timestamp) {
            this.registrationTimestamp = timestamp;
            return this;
        }

        public User build() {
            return new User(userId, username, fullName, passwordHash, status, registrationTimestamp);
        }
    }
}
