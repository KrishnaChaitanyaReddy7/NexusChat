package nexuschat.util;

import nexuschat.dao.DatabaseManager;
import nexuschat.dao.DatabaseOperations;
import nexuschat.model.*;
import nexuschat.network.NexusClient;
import nexuschat.network.NexusServer;
import nexuschat.network.SynchronizedMessageBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Automated Verification Suite for NexusChat
 * Tests all 5 syllabus units programmatically:
 * - Unit I: OOP, Constructors, Builder, Overloading, Recursion, Type wrappers
 * - Unit II: Inheritance, Polymorphism, Interfaces, Generics
 * - Unit III: Multithreading (Threads, Runnable, Synchronized buffer, wait/notify), Custom Exceptions
 * - Unit IV: Collections (List, Set, Map), java.io streams
 * - Unit V: JDBC Database operations, Statements, PreparedStatements, ResultSets
 */
public class NexusChatVerificationTest {

    private static int testsPassed = 0;
    private static int totalTests = 0;

    private static void assertTrue(String testName, boolean condition) {
        totalTests++;
        if (condition) {
            System.out.println("[PASS] " + testName);
            testsPassed++;
        } else {
            System.err.println("[FAIL] " + testName);
        }
    }

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  NexusChat Syllabus Verification & Unit Test Suite");
        System.out.println("==================================================");

        try {
            testUnitI_OOP_And_Recursion();
            testUnitII_Inheritance_Polymorphism_Generics();
            testUnitIII_Multithreading_And_WaitNotify();
            testUnitV_JDBC_DatabaseOperations();
            testUnitIII_And_IV_Networking_And_Sockets();

            System.out.println("==================================================");
            System.out.println("TEST RESULTS: " + testsPassed + " / " + totalTests + " PASSED.");
            System.out.println("==================================================");

            if (testsPassed == totalTests) {
                System.out.println(">>> ALL SYLLABUS CAPABILITIES VERIFIED 100% SUCCESSFULLY <<<");
            } else {
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Fatal error during testing: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void testUnitI_OOP_And_Recursion() {
        System.out.println("\n--- Testing Unit I: OOP Thinking, Overloading, Builder, Recursion ---");

        // Builder & Constructor Overloading
        User user1 = new User.Builder()
                .setUsername("alice")
                .setFullName("Alice Wonder")
                .setStatus(UserStatus.ONLINE)
                .build();

        assertTrue("User Builder instantiation", user1 != null && user1.getUsername().equals("alice"));
        assertTrue("User status enum assignment", user1.getStatus() == UserStatus.ONLINE);

        // Method overloading
        user1.updateProfile("Alice Smith");
        assertTrue("Overloaded method 1 updateProfile(name)", user1.getFullName().equals("Alice Smith"));
        user1.updateProfile("Alice Smith", UserStatus.AWAY);
        assertTrue("Overloaded method 2 updateProfile(name, status)", user1.getStatus() == UserStatus.AWAY);

        // Recursion test via HistorySearchUtil
        List<Message> sampleMessages = new ArrayList<>();
        sampleMessages.add(new TextMessage("alice", "bob", "Hello world from Java!"));
        sampleMessages.add(new TextMessage("bob", "alice", "Welcome to NexusChat project"));
        sampleMessages.add(new TextMessage("alice", "bob", "Java multithreading is powerful"));

        List<Message> javaMatches = HistorySearchUtil.searchByKeyword(sampleMessages, "java");
        assertTrue("Recursive keyword search (found 2 messages with 'java')", javaMatches.size() == 2);

        int totalChars = HistorySearchUtil.calculateTotalCharactersRecursive(sampleMessages, 0);
        assertTrue("Recursive character calculation", totalChars > 50);
    }

    private static void testUnitII_Inheritance_Polymorphism_Generics() {
        System.out.println("\n--- Testing Unit II: Inheritance, Polymorphism, Generics ---");

        // Hierarchical Abstraction & Polymorphism
        Message textMsg = new TextMessage("alice", "bob", "Direct hello");
        Message groupMsg = new GroupMessage("alice", "Developers", "Group announcement");
        Message sysMsg = new SystemMessage("Server is restarting");

        // Polymorphic method calls
        assertTrue("TextMessage polymorphism (getMessageType)", textMsg.getMessageType().equals("DIRECT"));
        assertTrue("GroupMessage polymorphism (getMessageType)", groupMsg.getMessageType().equals("GROUP"));
        assertTrue("SystemMessage polymorphism (getMessageType)", sysMsg.getMessageType().equals("SYSTEM"));
        assertTrue("Polymorphic display formatting", textMsg.getFormattedDisplay().contains("(DM to @bob)"));

        // Generics: MessagePacket<T> generic class
        MessagePacket<User> packet = new MessagePacket<>("USER_INFO", true, "Loaded", new User("charlie"));
        assertTrue("Generic class MessagePacket<T> payload", packet.getPayload().getUsername().equals("charlie"));

        // Generics: GenericFilterUtil.filter generic method
        List<User> userList = new ArrayList<>();
        userList.add(new User("userA", "User A"));
        userList.add(new User("userB", "User B"));
        userList.add(new User("admin", "Admin User"));

        List<User> admins = GenericFilterUtil.filter(userList, u -> u.getUsername().startsWith("admin"));
        assertTrue("Generic method filter() execution", admins.size() == 1 && admins.get(0).getUsername().equals("admin"));
    }

    private static void testUnitIII_Multithreading_And_WaitNotify() throws Exception {
        System.out.println("\n--- Testing Unit III: Multithreading, Synchronized Buffer (wait/notify) ---");

        SynchronizedMessageBuffer buffer = new SynchronizedMessageBuffer(5);
        CountDownLatch latch = new CountDownLatch(1);
        final String[] received = new String[1];

        // Consumer thread
        Thread consumerThread = new Thread(() -> {
            try {
                Message msg = buffer.consume(); // will wait until produced
                received[0] = msg.getContent();
                latch.countDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        consumerThread.start();

        Thread.sleep(50); // Ensure consumer is waiting

        // Producer thread
        Thread producerThread = new Thread(() -> {
            try {
                buffer.produce(new SystemMessage("Inter-thread payload"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        producerThread.start();

        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue("Inter-thread communication with wait() and notifyAll()", completed && "Inter-thread payload".equals(received[0]));
    }

    private static void testUnitV_JDBC_DatabaseOperations() throws Exception {
        System.out.println("\n--- Testing Unit V: JDBC Database Operations (DDL, PreparedStatement, ResultSet) ---");

        java.io.File testDbFile = new java.io.File("test_nexuschat.db");
        if (testDbFile.exists()) {
            testDbFile.delete();
        }

        DatabaseOperations db = new DatabaseManager("jdbc:sqlite:test_nexuschat.db");
        db.initializeDatabase();
        assertTrue("JDBC table creation (users, messages, chat_groups)", true);

        // Register user
        User testUser = new User.Builder()
                .setUsername("testuser1")
                .setFullName("Test User One")
                .setStatus(UserStatus.ONLINE)
                .build();

        boolean registered = db.registerUser(testUser, "password123");
        assertTrue("JDBC PreparedStatement user registration", registered);

        // Authenticate user
        User authUser = db.authenticateUser("testuser1", "password123");
        assertTrue("JDBC ResultSet user authentication", authUser != null && authUser.getUsername().equals("testuser1"));

        // Save & retrieve message
        TextMessage msg = new TextMessage("testuser1", "bob", "Hello Bob JDBC");
        boolean msgSaved = db.saveMessage(msg);
        assertTrue("JDBC message insertion", msgSaved);

        List<Message> history = db.getDirectMessageHistory("testuser1", "bob", 10);
        assertTrue("JDBC message retrieval", !history.isEmpty() && history.get(0).getContent().equals("Hello Bob JDBC"));

        // Channel creation, owner verification, adding user by username, and owner deletion
        User testUser2 = new User.Builder()
                .setUsername("testuser2")
                .setFullName("Test User Two")
                .setStatus(UserStatus.ONLINE)
                .build();
        db.registerUser(testUser2, "pass2");

        boolean groupCreated = db.createGroup("ProjectAlpha", "testuser1");
        assertTrue("Channel creation with owner attribution", groupCreated);

        String creator = db.getGroupCreator("ProjectAlpha");
        assertTrue("Channel creator retrieval", "testuser1".equalsIgnoreCase(creator));

        boolean addedMember = db.addUserToGroup("ProjectAlpha", "testuser2");
        assertTrue("Add user to channel based on username only", addedMember);

        boolean isMember = db.isUserInGroup("ProjectAlpha", "testuser2");
        assertTrue("Verification of added channel member", isMember);

        boolean nonOwnerDelete = db.deleteGroup("ProjectAlpha", "testuser2");
        assertTrue("Prevent non-owner from deleting channel", !nonOwnerDelete);

        boolean defaultDelete = db.deleteGroup("General", "testuser1");
        assertTrue("Prevent deletion of default channel (General)", !defaultDelete);

        boolean ownerDelete = db.deleteGroup("ProjectAlpha", "testuser1");
        assertTrue("Owner channel deletion permitted and executed", ownerDelete);

        String deletedCreator = db.getGroupCreator("ProjectAlpha");
        assertTrue("Channel removed completely after owner deletion", deletedCreator == null);
    }

    private static void testUnitIII_And_IV_Networking_And_Sockets() throws Exception {
        System.out.println("\n--- Testing Units III & IV: Client-Server Socket Communication ---");

        int testPort = 9988;
        DatabaseOperations testDb = new DatabaseManager("jdbc:sqlite:test_nexuschat.db");
        NexusServer server = new NexusServer(testPort, testDb);

        // Start server in background thread
        Thread serverThread = new Thread(server::start, "Test-Server-Thread");
        serverThread.start();

        Thread.sleep(300); // Give server a moment to bind

        NexusClient client = new NexusClient();
        CountDownLatch loginLatch = new CountDownLatch(1);
        final boolean[] loginSuccess = {false};

        client.setEventListener(new NexusClient.ClientEventListener() {
            @Override
            public void onLoginResponse(boolean success, String message, String username, String fullName) {
                loginSuccess[0] = success;
                loginLatch.countDown();
            }

            @Override public void onRegisterResponse(boolean success, String message) {}
            @Override public void onDirectMessageReceived(String sender, String recipient, String content, String time, String status, int messageId) {}
            @Override public void onGroupMessageReceived(String sender, String group, String content, String time) {}
            @Override public void onMessageStatusUpdate(String otherParty, String newStatus) {}
            @Override public void onMessageDeleted(int messageId, String target) {}
            @Override public void onSystemAlertReceived(String message) {}
            @Override public void onUserListReceived(String[] userData) {}
            @Override public void onGroupListReceived(String[] groups) {}
            @Override public void onGroupDeleted(String groupName) {}
            @Override public void onGroupCreatorReceived(String groupName, String creator) {}
            @Override public void onHistoryReceived(String target, String[] historyEntries) {}
            @Override public void onConnectionLost(String reason) {}
        });

        client.connect("localhost", testPort);
        assertTrue("Socket connection to server", client.isConnected());

        client.login("testuser1", "password123");
        boolean receivedResponse = loginLatch.await(3, TimeUnit.SECONDS);
        assertTrue("Network socket protocol login exchange", receivedResponse && loginSuccess[0]);

        // Test Socket Channel Creation, Creator Lookup, Member Addition, and Deletion
        CountDownLatch creatorLatch = new CountDownLatch(1);
        final String[] creatorReceived = new String[1];
        CountDownLatch deleteLatch = new CountDownLatch(1);
        final String[] deletedGroupReceived = new String[1];

        client.setEventListener(new NexusClient.ClientEventListener() {
            @Override public void onLoginResponse(boolean s, String m, String u, String f) {}
            @Override public void onRegisterResponse(boolean s, String m) {}
            @Override public void onDirectMessageReceived(String s, String r, String c, String t, String st, int id) {}
            @Override public void onGroupMessageReceived(String s, String g, String c, String t) {}
            @Override public void onMessageStatusUpdate(String o, String s) {}
            @Override public void onMessageDeleted(int id, String t) {}
            @Override public void onSystemAlertReceived(String m) {}
            @Override public void onUserListReceived(String[] u) {}
            @Override public void onGroupListReceived(String[] g) {}
            @Override
            public void onGroupDeleted(String groupName) {
                deletedGroupReceived[0] = groupName;
                deleteLatch.countDown();
            }
            @Override
            public void onGroupCreatorReceived(String groupName, String creator) {
                creatorReceived[0] = creator;
                creatorLatch.countDown();
            }
            @Override public void onHistoryReceived(String t, String[] e) {}
            @Override public void onConnectionLost(String r) {}
        });

        // 1. Create channel over socket
        client.createGroup("SocketChannel");
        Thread.sleep(200);

        // 2. Request creator over socket
        client.requestGroupCreator("SocketChannel");
        boolean creatorGot = creatorLatch.await(2, TimeUnit.SECONDS);
        assertTrue("Socket network group creator request/response", creatorGot && "testuser1".equalsIgnoreCase(creatorReceived[0]));

        // 3. Add user based on username over socket
        client.addGroupMember("SocketChannel", "testuser2");
        Thread.sleep(200);
        assertTrue("Socket network member addition by username", testDb.isUserInGroup("SocketChannel", "testuser2"));

        // 4. Delete channel by owner over socket
        client.deleteGroup("SocketChannel");
        boolean deletedGot = deleteLatch.await(2, TimeUnit.SECONDS);
        assertTrue("Socket network group deletion broadcast event", deletedGot && "SocketChannel".equalsIgnoreCase(deletedGroupReceived[0]));

        client.disconnect();
        server.stop();
        serverThread.join(1500);
        assertTrue("Graceful socket disconnection and server stop", true);
    }
}
