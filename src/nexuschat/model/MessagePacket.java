package nexuschat.model;

import java.io.Serializable;

/**
 * Unit II Syllabus:
 * - Introduction to Generics
 * - Generic classes: Class parameterized by type <T>
 * - Type safety and elimination of explicit type casting
 */
public class MessagePacket<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private String command;
    private boolean success;
    private String message;
    private T payload; // Generic payload of type T

    // Overloaded Constructor 1
    public MessagePacket(String command) {
        this(command, true, "", null);
    }

    // Overloaded Constructor 2
    public MessagePacket(String command, boolean success, String message) {
        this(command, success, message, null);
    }

    // Overloaded Constructor 3: Full generic constructor
    public MessagePacket(String command, boolean success, String message, T payload) {
        this.command = command;
        this.success = success;
        this.message = message;
        this.payload = payload;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Generic getter returning type T
    public T getPayload() {
        return payload;
    }

    // Generic setter taking parameter of type T
    public void setPayload(T payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "MessagePacket [cmd=" + command + ", success=" + success + ", msg=" + message + ", payload=" + payload + "]";
    }
}
