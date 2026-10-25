package nexuschat.model;

/**
 * Unit II Syllabus:
 * - Inheritance (Extension of Message abstraction)
 * - Polymorphism via method overriding
 */
public class GroupMessage extends Message {
    private static final long serialVersionUID = 1L;

    private String groupName;

    public GroupMessage(String sender, String groupName, String content) {
        super(sender, content);
        this.groupName = (groupName != null) ? groupName.trim() : "General";
    }

    public GroupMessage(int messageId, String sender, String groupName, String content, long timestamp) {
        super(messageId, sender, content, timestamp);
        this.groupName = (groupName != null) ? groupName.trim() : "General";
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public String getMessageType() {
        return "GROUP";
    }

    @Override
    public String getFormattedDisplay() {
        return "[" + getFormattedTime() + "] [#" + groupName + "] " + sender + ": " + content;
    }
}
