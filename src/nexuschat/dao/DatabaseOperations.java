package nexuschat.dao;

import nexuschat.model.Message;
import nexuschat.model.User;
import java.util.List;

/**
 * Unit II Syllabus:
 * - Defining an interface
 * - Decoupling implementation from specification
 * 
 * Unit V Syllabus:
 * - Database connectivity abstraction
 */
public interface DatabaseOperations {

    // Initialize database tables using JDBC DDL statements
    void initializeDatabase() throws Exception;

    // User authentication and registration
    boolean registerUser(User user, String password) throws Exception;
    User authenticateUser(String username, String password) throws Exception;
    List<User> getAllUsers() throws Exception;
    boolean updateUserStatus(String username, String status) throws Exception;

    // Message persistence
    boolean saveMessage(Message message) throws Exception;
    boolean deleteMessage(int messageId, String username) throws Exception;
    List<Message> getDirectMessageHistory(String userA, String userB, int limit) throws Exception;
    List<Message> getGroupMessageHistory(String groupName, int limit) throws Exception;
    void markMessagesAsRead(String sender, String recipient) throws Exception;
    void markMessagesAsDelivered(String recipient) throws Exception;
    List<String> getSendersWithPendingDelivered(String recipient) throws Exception;

    // Group management
    boolean createGroup(String groupName, String creator) throws Exception;
    List<String> getAllGroups() throws Exception;
    String getGroupCreator(String groupName) throws Exception;
    boolean deleteGroup(String groupName, String requester) throws Exception;
    boolean addUserToGroup(String groupName, String username) throws Exception;
    List<String> getGroupMembers(String groupName) throws Exception;
    boolean isUserInGroup(String groupName, String username) throws Exception;
    boolean userExists(String username) throws Exception;
}
