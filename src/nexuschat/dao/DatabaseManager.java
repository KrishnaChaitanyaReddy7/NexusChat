package nexuschat.dao;

import nexuschat.model.*;
import nexuschat.service.AesEncryptionService;
import nexuschat.service.EncryptionService;
import nexuschat.util.NexusConstants;
import nexuschat.util.SecurityUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit V Syllabus:
 * - JDBC Architecture: Application -> DriverManager -> Driver -> Database
 * - Types of Drivers:
 *     1. Type-1: JDBC-ODBC Bridge driver
 *     2. Type-2: Native-API driver (part Java, part native C/C++)
 *     3. Type-3: Network-Protocol driver (all Java, middleware)
 *     4. Type-4: Thin driver (Direct-to-Database Pure Java Driver - e.g. SQLite / MySQL)
 * - Basic steps in developing JDBC applications:
 *     Step 1: Load and register the JDBC Driver
 *     Step 2: Create a Connection object via DriverManager
 *     Step 3: Create Statement or PreparedStatement objects
 *     Step 4: Execute SQL queries and updates
 *     Step 5: Process the ResultSet and ResultSetMetaData
 *     Step 6: Close JDBC resources (try-with-resources / finally)
 * - Creating database and tables using JDBC DDL statements
 */
public class DatabaseManager implements DatabaseOperations {

    private final String dbUrl;
    private final EncryptionService encryptionService = new AesEncryptionService();

    public DatabaseManager() {
        this(NexusConstants.DB_URL);
    }

    public DatabaseManager(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    /**
     * Step 1 & 2: Load Driver and Establish Connection
     */
    private Connection getConnection() throws SQLException, ClassNotFoundException {
        // Step 1: Load Driver class
        try {
            Class.forName(NexusConstants.DB_DRIVER);
        } catch (ClassNotFoundException e) {
            System.err.println("[JDBC WARNING] Driver class not found: " + e.getMessage());
            throw e;
        }
        // Step 2: Establish Connection via DriverManager
        return DriverManager.getConnection(dbUrl);
    }

    /**
     * Creating new tables with JDBC Statement DDL
     */
    @Override
    public void initializeDatabase() throws Exception {
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
                + "user_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT UNIQUE NOT NULL, "
                + "full_name TEXT NOT NULL, "
                + "password_hash TEXT NOT NULL, "
                + "status TEXT DEFAULT 'OFFLINE', "
                + "created_at INTEGER NOT NULL"
                + ");";

        String createMessagesTable = "CREATE TABLE IF NOT EXISTS messages ("
                + "message_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "message_type TEXT NOT NULL, "
                + "sender TEXT NOT NULL, "
                + "recipient TEXT, "
                + "group_name TEXT, "
                + "content TEXT NOT NULL, "
                + "timestamp INTEGER NOT NULL, "
                + "status TEXT DEFAULT 'SENT'"
                + ");";

        String createGroupsTable = "CREATE TABLE IF NOT EXISTS chat_groups ("
                + "group_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "group_name TEXT UNIQUE NOT NULL, "
                + "created_by TEXT NOT NULL, "
                + "created_at INTEGER NOT NULL"
                + ");";

        String createGroupMembersTable = "CREATE TABLE IF NOT EXISTS group_members ("
                + "group_name TEXT NOT NULL, "
                + "username TEXT NOT NULL, "
                + "joined_at INTEGER NOT NULL, "
                + "PRIMARY KEY (group_name, username)"
                + ");";

        // Step 3 & 4: Create Statement and execute DDL updates using try-with-resources
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(createUsersTable);
            stmt.executeUpdate(createMessagesTable);
            stmt.executeUpdate(createGroupsTable);
            stmt.executeUpdate(createGroupMembersTable);

            // Attempt to upgrade existing tables with status column if not present
            try {
                stmt.executeUpdate("ALTER TABLE messages ADD COLUMN status TEXT DEFAULT 'SENT';");
            } catch (SQLException ignored) {}

            // Insert default 'General' and 'Developers' groups if not existing
            stmt.executeUpdate("INSERT OR IGNORE INTO chat_groups (group_name, created_by, created_at) "
                    + "VALUES ('General', 'SYSTEM', " + System.currentTimeMillis() + ");");
            stmt.executeUpdate("INSERT OR IGNORE INTO chat_groups (group_name, created_by, created_at) "
                    + "VALUES ('Developers', 'SYSTEM', " + System.currentTimeMillis() + ");");

            System.out.println("[DatabaseManager] Database initialized and tables verified successfully.");
        }
    }

    /**
     * Unit V: JDBC PreparedStatement for parameterized queries to prevent SQL Injection
     */
    @Override
    public boolean registerUser(User user, String password) throws Exception {
        String sql = "INSERT INTO users (username, full_name, password_hash, status, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getFullName());
            pstmt.setString(3, SecurityUtil.hashPassword(password));
            pstmt.setString(4, user.getStatus().name());
            pstmt.setLong(5, user.getRegistrationTimestamp());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException ex) {
            // Check for UNIQUE constraint violation (Error code or message)
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("unique")) {
                return false;
            }
            throw ex;
        }
    }

    @Override
    public User authenticateUser(String username, String password) throws Exception {
        String sql = "SELECT user_id, username, full_name, password_hash, status, created_at FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            // Step 5: Process the ResultSet
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (!SecurityUtil.verifyPassword(password, storedHash)) {
                        return null;
                    }

                    int id = rs.getInt("user_id");
                    String uname = rs.getString("username");
                    String fullName = rs.getString("full_name");
                    String statusStr = rs.getString("status");
                    long createdAt = rs.getLong("created_at");

                    UserStatus status = UserStatus.ONLINE;
                    try {
                        status = UserStatus.valueOf(statusStr);
                    } catch (Exception ignored) {}

                    return new User(id, uname, fullName, storedHash, status, createdAt);
                }
            }
        }
        return null;
    }

    @Override
    public boolean updateUserStatus(String username, String status) throws Exception {
        String sql = "UPDATE users SET status = ? WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setString(2, username);
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public List<User> getAllUsers() throws Exception {
        List<User> list = new ArrayList<>();
        String sql = "SELECT user_id, username, full_name, status, created_at FROM users ORDER BY username ASC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // Step 5: Inspect ResultSetMetaData (Unit V Syllabus requirement!)
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            // System.out.println("[DatabaseManager] Fetched columns count: " + columnCount);

            while (rs.next()) {
                int id = rs.getInt("user_id");
                String uname = rs.getString("username");
                String fullName = rs.getString("full_name");
                String statusStr = rs.getString("status");
                long createdAt = rs.getLong("created_at");

                UserStatus status = UserStatus.OFFLINE;
                try {
                    status = UserStatus.valueOf(statusStr);
                } catch (Exception ignored) {}

                list.add(new User(id, uname, fullName, "", status, createdAt));
            }
        }
        return list;
    }

    @Override
    public boolean saveMessage(Message message) throws Exception {
        String sql = "INSERT INTO messages (message_type, sender, recipient, group_name, content, timestamp, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, message.getMessageType());
            pstmt.setString(2, message.getSender());

            String status = "SENT";
            if (message instanceof TextMessage) {
                // Type casting and instanceof operator (Unit I & II)
                TextMessage tm = (TextMessage) message;
                pstmt.setString(3, tm.getRecipient());
                pstmt.setNull(4, Types.VARCHAR);
                status = tm.getStatus();
            } else if (message instanceof GroupMessage) {
                GroupMessage gm = (GroupMessage) message;
                pstmt.setNull(3, Types.VARCHAR);
                pstmt.setString(4, gm.getGroupName());
            } else {
                pstmt.setNull(3, Types.VARCHAR);
                pstmt.setNull(4, Types.VARCHAR);
            }

            pstmt.setString(5, encryptionService.encrypt(message.getContent()));
            pstmt.setLong(6, message.getTimestamp());
            pstmt.setString(7, status);

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        message.setMessageId(keys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean deleteMessage(int messageId, String username) throws Exception {
        String sql = "DELETE FROM messages WHERE message_id = ? AND sender = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, messageId);
            pstmt.setString(2, username);
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public List<Message> getDirectMessageHistory(String userA, String userB, int limit) throws Exception {
        List<Message> history = new ArrayList<>();
        String sql = "SELECT message_id, message_type, sender, recipient, content, timestamp, status "
                + "FROM messages "
                + "WHERE (sender = ? AND recipient = ?) OR (sender = ? AND recipient = ?) "
                + "ORDER BY timestamp ASC LIMIT ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userA);
            pstmt.setString(2, userB);
            pstmt.setString(3, userB);
            pstmt.setString(4, userA);
            pstmt.setInt(5, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("message_id");
                    String sender = rs.getString("sender");
                    String recipient = rs.getString("recipient");
                    String content = encryptionService.decrypt(rs.getString("content"));
                    long timestamp = rs.getLong("timestamp");
                    String status = rs.getString("status");

                    history.add(new TextMessage(id, sender, recipient, content, timestamp, status));
                }
            }
        }
        return history;
    }

    @Override
    public void markMessagesAsRead(String sender, String recipient) throws Exception {
        String sql = "UPDATE messages SET status = 'READ' WHERE sender = ? AND recipient = ? AND status != 'READ'";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sender);
            pstmt.setString(2, recipient);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void markMessagesAsDelivered(String recipient) throws Exception {
        String sql = "UPDATE messages SET status = 'DELIVERED' WHERE recipient = ? AND status = 'SENT'";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, recipient);
            pstmt.executeUpdate();
        }
    }

    @Override
    public List<String> getSendersWithPendingDelivered(String recipient) throws Exception {
        List<String> senders = new ArrayList<>();
        String sql = "SELECT DISTINCT sender FROM messages WHERE recipient = ? AND status = 'DELIVERED'";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, recipient);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    senders.add(rs.getString("sender"));
                }
            }
        }
        return senders;
    }

    @Override
    public List<Message> getGroupMessageHistory(String groupName, int limit) throws Exception {
        List<Message> history = new ArrayList<>();
        String sql = "SELECT message_id, message_type, sender, group_name, content, timestamp "
                + "FROM messages "
                + "WHERE group_name = ? "
                + "ORDER BY timestamp ASC LIMIT ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, groupName);
            pstmt.setInt(2, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("message_id");
                    String sender = rs.getString("sender");
                    String grp = rs.getString("group_name");
                    String content = encryptionService.decrypt(rs.getString("content"));
                    long timestamp = rs.getLong("timestamp");

                    history.add(new GroupMessage(id, sender, grp, content, timestamp));
                }
            }
        }
        return history;
    }

    @Override
    public boolean createGroup(String groupName, String creator) throws Exception {
        String sql = "INSERT INTO chat_groups (group_name, created_by, created_at) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, groupName);
            pstmt.setString(2, creator);
            pstmt.setLong(3, System.currentTimeMillis());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                // Automatically add creator to group members
                try {
                    addUserToGroup(groupName, creator);
                } catch (Exception ignored) {}
                return true;
            }
            return false;
        } catch (SQLException ex) {
            return false;
        }
    }

    @Override
    public List<String> getAllGroups() throws Exception {
        List<String> groups = new ArrayList<>();
        String sql = "SELECT group_name FROM chat_groups ORDER BY group_name ASC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                groups.add(rs.getString("group_name"));
            }
        }
        return groups;
    }

    @Override
    public boolean userExists(String username) throws Exception {
        if (username == null || username.trim().isEmpty()) return false;
        String sql = "SELECT 1 FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public String getGroupCreator(String groupName) throws Exception {
        if (groupName == null) return null;
        String sql = "SELECT created_by FROM chat_groups WHERE LOWER(group_name) = LOWER(?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, groupName.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("created_by");
                }
            }
        }
        return null;
    }

    /**
     * Unit V: JDBC Transaction Management (setAutoCommit(false), commit(), rollback())
     * Ensures group metadata, membership, and message history are deleted atomically.
     */
    @Override
    public boolean deleteGroup(String groupName, String requester) throws Exception {
        if (groupName == null || requester == null) return false;
        String creator = getGroupCreator(groupName);
        if (creator == null) return false;
        if ("SYSTEM".equalsIgnoreCase(creator)) return false; // Default channels cannot be deleted
        if (!creator.equalsIgnoreCase(requester.trim())) return false; // Only creator can delete

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement pstmtGrp = conn.prepareStatement("DELETE FROM chat_groups WHERE LOWER(group_name) = LOWER(?)")) {
                    pstmtGrp.setString(1, groupName.trim());
                    pstmtGrp.executeUpdate();
                }
                try (PreparedStatement pstmtMem = conn.prepareStatement("DELETE FROM group_members WHERE LOWER(group_name) = LOWER(?)")) {
                    pstmtMem.setString(1, groupName.trim());
                    pstmtMem.executeUpdate();
                }
                try (PreparedStatement pstmtMsg = conn.prepareStatement("DELETE FROM messages WHERE LOWER(group_name) = LOWER(?)")) {
                    pstmtMsg.setString(1, groupName.trim());
                    pstmtMsg.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public boolean addUserToGroup(String groupName, String username) throws Exception {
        if (groupName == null || username == null) return false;
        if (!userExists(username.trim())) return false;

        String sql = "INSERT OR IGNORE INTO group_members (group_name, username, joined_at) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, groupName.trim());
            pstmt.setString(2, username.trim());
            pstmt.setLong(3, System.currentTimeMillis());
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean isUserInGroup(String groupName, String username) throws Exception {
        if (groupName == null || username == null) return false;
        String sql = "SELECT 1 FROM group_members WHERE LOWER(group_name) = LOWER(?) AND LOWER(username) = LOWER(?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, groupName.trim());
            pstmt.setString(2, username.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public List<String> getGroupMembers(String groupName) throws Exception {
        List<String> members = new ArrayList<>();
        if (groupName == null) return members;
        String sql = "SELECT username FROM group_members WHERE LOWER(group_name) = LOWER(?) ORDER BY username ASC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, groupName.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    members.add(rs.getString("username"));
                }
            }
        }
        return members;
    }
}
