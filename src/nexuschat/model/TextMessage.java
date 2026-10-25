package nexuschat.model;

/**
 * Unit II Syllabus:
 * - Inheritance (Specialization)
 * - 'super' keyword: calling superclass constructor and methods
 * - Method overriding (Polymorphism)
 * 
 * WhatsApp-Style Status Ticks:
 * - SENT: 1 tick (✓) - stored on server, recipient offline
 * - DELIVERED: 2 ticks (✓✓ grey) - delivered to recipient's active device
 * - READ: 2 ticks (✓✓ blue) - recipient opened/read the message
 */
public class TextMessage extends Message {
    private static final long serialVersionUID = 1L;

    private String recipient; // The username of recipient (for one-on-one direct chat)
    private String status = "SENT"; // SENT, DELIVERED, READ

    // Overloaded Constructor 1
    public TextMessage(String sender, String recipient, String content) {
        super(sender, content);
        this.recipient = (recipient != null) ? recipient.trim() : "All";
        this.status = "SENT";
    }

    // Overloaded Constructor 2
    public TextMessage(int messageId, String sender, String recipient, String content, long timestamp) {
        this(messageId, sender, recipient, content, timestamp, "SENT");
    }

    // Overloaded Constructor 3: With status
    public TextMessage(int messageId, String sender, String recipient, String content, long timestamp, String status) {
        super(messageId, sender, content, timestamp);
        this.recipient = (recipient != null) ? recipient.trim() : "All";
        this.status = (status != null && !status.isEmpty()) ? status : "SENT";
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = (status != null) ? status : "SENT";
    }

    public String getStatusTickText() {
        if ("READ".equalsIgnoreCase(status)) {
            return "✓✓ (Read)";
        } else if ("DELIVERED".equalsIgnoreCase(status)) {
            return "✓✓";
        } else {
            return "✓";
        }
    }

    // Unit II: Method Overriding (Polymorphic behavior)
    @Override
    public String getMessageType() {
        return "DIRECT";
    }

    @Override
    public String getFormattedDisplay() {
        return "[" + getFormattedTime() + "] (DM to @" + recipient + ") @" + sender + ": " + content + " " + getStatusTickText();
    }
}
