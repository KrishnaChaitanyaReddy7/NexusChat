package nexuschat.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Unit II Syllabus:
 * - Hierarchical Abstraction (Inheritance - Specialization)
 * - Abstract classes & Method overriding (Polymorphism)
 * - Member access rules: protected fields accessible by subclasses
 * - 'super' keyword usage in subclasses
 * 
 * Unit IV Syllabus:
 * - java.io.Serializable
 */
public abstract class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    // Protected members accessible to inheriting subclasses
    protected int messageId;
    protected String sender;
    protected long timestamp;
    protected String content;

    // Overloaded base constructors
    public Message(String sender, String content) {
        this(0, sender, content, System.currentTimeMillis());
    }

    public Message(int messageId, String sender, String content, long timestamp) {
        this.messageId = messageId;
        this.sender = (sender != null) ? sender : "Anonymous";
        this.content = (content != null) ? content : "";
        this.timestamp = (timestamp > 0) ? timestamp : System.currentTimeMillis();
    }

    // Abstract methods defining polymorphic interface for all message types
    public abstract String getFormattedDisplay();
    public abstract String getMessageType();

    // Helper method to format timestamp
    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }

    // Standard getters and setters
    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "[" + getFormattedTime() + "] " + getMessageType() + " from " + sender + ": " + content;
    }
}
