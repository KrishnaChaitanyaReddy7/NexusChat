package nexuschat.model;

/**
 * Unit II Syllabus:
 * - Inheritance: Specialization for broadcast system notifications
 * - Polymorphism via method overriding
 */
public class SystemMessage extends Message {
    private static final long serialVersionUID = 1L;

    public SystemMessage(String content) {
        super("SYSTEM", content);
    }

    public SystemMessage(int messageId, String content, long timestamp) {
        super(messageId, "SYSTEM", content, timestamp);
    }

    @Override
    public String getMessageType() {
        return "SYSTEM";
    }

    @Override
    public String getFormattedDisplay() {
        return ">>> [" + getFormattedTime() + "] SYSTEM NOTICE: " + content;
    }
}
