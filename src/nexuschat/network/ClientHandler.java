package nexuschat.network;

import nexuschat.dao.DatabaseOperations;
import nexuschat.model.*;
import nexuschat.util.NexusConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

/**
 * Unit III Syllabus:
 * - Multithreading: Creating threads by extending Thread class
 * - Thread lifecycle: RUNNABLE -> WAITING -> TERMINATED
 * - Thread priorities: Setting thread priority
 * 
 * Unit IV Syllabus:
 * - java.io package: BufferedReader, InputStreamReader, PrintWriter
 * - String splitting, tokenizing, and validation
 */
public class ClientHandler extends Thread {

    private final Socket socket;
    private final NexusServer server;
    private final DatabaseOperations dbOps;

    private BufferedReader reader;
    private PrintWriter writer;
    private User currentUser;
    private boolean isRunning = true;
    private String currentActiveChat = "General";

    public String getCurrentActiveChat() {
        return currentActiveChat;
    }

    public void setCurrentActiveChat(String chat) {
        this.currentActiveChat = (chat != null) ? chat.trim() : "General";
    }

    /**
     * Unit III: Thread constructor taking a ThreadGroup, Runnable/Name
     */
    public ClientHandler(ThreadGroup group, Socket socket, NexusServer server, DatabaseOperations dbOps) {
        super(group, "ClientHandler-" + socket.getRemoteSocketAddress());
        this.socket = socket;
        this.server = server;
        this.dbOps = dbOps;
        
        // Unit III: Setting thread priority (1 to 10 scale)
        this.setPriority(Thread.NORM_PRIORITY);
    }

    @Override
    public void run() {
        try {
            // Unit IV: java.io Package - Stream readers and writers for socket communication
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            String inputLine;
            while (isRunning && (inputLine = reader.readLine()) != null) {
                processCommand(inputLine.trim());
            }
        } catch (IOException e) {
            System.out.println("[ClientHandler] Connection reset/closed for: " + (currentUser != null ? currentUser.getUsername() : socket.getRemoteSocketAddress()));
        } finally {
            cleanup();
        }
    }

    private void processCommand(String commandLine) {
        if (commandLine.isEmpty()) return;

        // Command format: COMMAND||ARG1||ARG2||...
        String[] parts = commandLine.split("\\|\\|");
        String cmd = parts[0];

        try {
            switch (cmd) {
                case NetworkProtocol.CMD_LOGIN:
                    handleLogin(parts);
                    break;
                case NetworkProtocol.CMD_REGISTER:
                    handleRegister(parts);
                    break;
                case NetworkProtocol.CMD_MSG_DIRECT:
                    handleDirectMessage(parts);
                    break;
                case NetworkProtocol.CMD_MSG_GROUP:
                    handleGroupMessage(parts);
                    break;
                case NetworkProtocol.CMD_GET_USERS:
                    sendUserList();
                    break;
                case NetworkProtocol.CMD_GET_GROUPS:
                    sendGroupList();
                    break;
                case NetworkProtocol.CMD_CREATE_GROUP:
                    handleCreateGroup(parts);
                    break;
                case NetworkProtocol.CMD_DELETE_GROUP:
                    handleDeleteGroup(parts);
                    break;
                case NetworkProtocol.CMD_ADD_GROUP_MEMBER:
                    handleAddGroupMember(parts);
                    break;
                case NetworkProtocol.CMD_GET_GROUP_CREATOR:
                    handleGetGroupCreator(parts);
                    break;
                case NetworkProtocol.CMD_GET_HISTORY:
                    handleGetHistory(parts);
                    break;
                case NetworkProtocol.CMD_MSG_READ:
                    handleMarkRead(parts);
                    break;
                case NetworkProtocol.CMD_DELETE_MSG:
                    handleDeleteMessage(parts);
                    break;
                case NetworkProtocol.CMD_STATUS_UPDATE:
                    handleStatusUpdate(parts);
                    break;
                case NetworkProtocol.CMD_LOGOUT:
                    isRunning = false;
                    break;
                default:
                    sendMessagePacket(NetworkProtocol.CMD_ERROR, "Unknown protocol command: " + cmd);
            }
        } catch (Exception ex) {
            sendMessagePacket(NetworkProtocol.CMD_ERROR, "Error executing request: " + ex.getMessage());
        }
    }

    private void handleLogin(String[] parts) throws Exception {
        if (parts.length < 3) {
            sendMessagePacket(NetworkProtocol.CMD_ERROR, "Invalid LOGIN arguments");
            return;
        }
        String username = parts[1].trim();
        String password = parts[2].trim();

        User user = dbOps.authenticateUser(username, password);
        if (user != null) {
            this.currentUser = user;
            this.currentUser.setStatus(UserStatus.ONLINE);
            dbOps.updateUserStatus(username, UserStatus.ONLINE.name());

            server.registerClient(username, this);
            sendMessagePacket(NetworkProtocol.CMD_SUCCESS, "Login successful||" + user.getUsername() + "||" + user.getFullName());

            // Mark any pending messages sent to this user as DELIVERED in the database
            dbOps.markMessagesAsDelivered(user.getUsername());
            List<String> senders = dbOps.getSendersWithPendingDelivered(user.getUsername());
            for (String sender : senders) {
                server.notifyStatusUpdate(sender, user.getUsername(), "DELIVERED");
            }

            // Broadcast join notification to all users
            server.broadcastSystemMessage(user.getUsername() + " has joined NexusChat.");
            server.broadcastUserList();
        } else {
            sendMessagePacket(NetworkProtocol.CMD_ERROR, "Invalid username or password.");
        }
    }

    private void handleRegister(String[] parts) throws Exception {
        if (parts.length < 4) {
            sendMessagePacket(NetworkProtocol.CMD_ERROR, "Invalid REGISTER arguments");
            return;
        }
        String username = parts[1].trim();
        String fullName = parts[2].trim();
        String password = parts[3].trim();

        if (username.length() < 3 || password.length() < NexusConstants.MIN_PASSWORD_LENGTH) {
            sendMessagePacket(NetworkProtocol.CMD_ERROR, "Username must be >= 3 chars, password >= " + NexusConstants.MIN_PASSWORD_LENGTH + " chars.");
            return;
        }

        // Unit I: Using User.Builder static nested class
        User newUser = new User.Builder()
                .setUsername(username)
                .setFullName(fullName)
                .setStatus(UserStatus.ONLINE)
                .build();

        boolean registered = dbOps.registerUser(newUser, password);
        if (registered) {
            sendMessagePacket(NetworkProtocol.CMD_SUCCESS, "Registration successful! You may now log in.");
        } else {
            sendMessagePacket(NetworkProtocol.CMD_ERROR, "Username already exists. Please choose a different username.");
        }
    }

    private void handleDirectMessage(String[] parts) throws Exception {
        if (currentUser == null) {
            sendMessagePacket(NetworkProtocol.CMD_ERROR, "Please log in first.");
            return;
        }
        if (parts.length < 3) return;
        String recipient = parts[1].trim();
        String content = parts[2];

        // Determine message status: SENT (offline), DELIVERED (online), or READ (active chat open)
        String status = "SENT";
        boolean isOnline = server.isUserOnline(recipient);
        ClientHandler recipientHandler = server.getClient(recipient);

        if (isOnline && recipientHandler != null) {
            if (currentUser.getUsername().equalsIgnoreCase(recipientHandler.getCurrentActiveChat())) {
                status = "READ"; // Recipient is actively viewing this conversation!
            } else {
                status = "DELIVERED"; // Recipient is online but viewing another chat
            }
        }

        TextMessage message = new TextMessage(0, currentUser.getUsername(), recipient, content, System.currentTimeMillis(), status);
        dbOps.saveMessage(message);

        // Forward to recipient if online
        if (isOnline) {
            server.sendDirectMessage(recipient, message);
        }

        // Echo back to sender with live status and messageId
        sendRawLine(NetworkProtocol.CMD_MSG_DIRECT + "||" + message.getSender() + "||" + message.getRecipient() + "||" + message.getContent() + "||" + message.getFormattedTime() + "||" + status + "||" + message.getMessageId());
    }

    private void handleDeleteMessage(String[] parts) throws Exception {
        if (currentUser == null || parts.length < 4) return;
        int messageId = Integer.parseInt(parts[1].trim());
        String target = parts[2].trim();
        boolean isGroup = Boolean.parseBoolean(parts[3].trim());

        boolean deleted = dbOps.deleteMessage(messageId, currentUser.getUsername());
        if (deleted) {
            // Echo confirmation to sender
            sendRawLine(NetworkProtocol.CMD_MSG_DELETED + "||" + messageId + "||" + target);

            if (isGroup) {
                server.broadcastGroupMessageDeleted(target, messageId);
            } else {
                // Notify recipient
                server.notifyMessageDeleted(target, messageId, currentUser.getUsername());
            }
        }
    }

    private void handleGroupMessage(String[] parts) throws Exception {
        if (currentUser == null) return;
        if (parts.length < 3) return;
        String groupName = parts[1].trim();
        String content = parts[2];

        GroupMessage message = new GroupMessage(currentUser.getUsername(), groupName, content);
        dbOps.saveMessage(message);

        // Broadcast to all participants
        server.broadcastGroupMessage(groupName, message);
    }

    private void handleCreateGroup(String[] parts) throws Exception {
        if (parts.length < 2 || currentUser == null) return;
        String groupName = parts[1].trim();

        boolean created = dbOps.createGroup(groupName, currentUser.getUsername());
        if (created) {
            sendMessagePacket(NetworkProtocol.CMD_SUCCESS, "Group '" + groupName + "' created.");
            server.broadcastGroupList();
        } else {
            sendMessagePacket(NetworkProtocol.CMD_ERROR, "Group already exists or could not be created.");
        }
    }

    private void handleDeleteGroup(String[] parts) throws Exception {
        if (currentUser == null || parts.length < 2) return;
        String groupName = parts[1].trim();

        if (groupName.equalsIgnoreCase("General") || groupName.equalsIgnoreCase("Developers")) {
            sendMessagePacket(NetworkProtocol.CMD_ERROR, "Default channel #" + groupName + " cannot be deleted.");
            return;
        }

        String creator = dbOps.getGroupCreator(groupName);
        if (creator == null) {
            sendMessagePacket(NetworkProtocol.CMD_ERROR, "Channel #" + groupName + " does not exist.");
            return;
        }

        if (!creator.equalsIgnoreCase(currentUser.getUsername())) {
            sendMessagePacket(NetworkProtocol.CMD_ERROR, "Only the channel creator (@" + creator + ") can delete #" + groupName + ".");
            return;
        }

        boolean deleted = dbOps.deleteGroup(groupName, currentUser.getUsername());
        if (deleted) {
            server.broadcastGroupDeleted(groupName);
            server.broadcastGroupList();
            sendMessagePacket(NetworkProtocol.CMD_SUCCESS, "Channel #" + groupName + " was deleted.");
        } else {
            sendMessagePacket(NetworkProtocol.CMD_ERROR, "Failed to delete channel #" + groupName + ".");
        }
    }

    private void handleAddGroupMember(String[] parts) throws Exception {
        if (currentUser == null || parts.length < 3) return;
        String groupName = parts[1].trim();
        String targetUsername = parts[2].trim();

        if (!dbOps.userExists(targetUsername)) {
            sendMessagePacket(NetworkProtocol.CMD_ERROR, "User @" + targetUsername + " does not exist.");
            return;
        }

        if (dbOps.isUserInGroup(groupName, targetUsername)) {
            sendMessagePacket(NetworkProtocol.CMD_ERROR, "@" + targetUsername + " is already in #" + groupName + ".");
            return;
        }

        boolean added = dbOps.addUserToGroup(groupName, targetUsername);
        if (added) {
            sendMessagePacket(NetworkProtocol.CMD_SUCCESS, "Added @" + targetUsername + " to #" + groupName + ".");

            // Post system announcement inside the channel
            GroupMessage sysNotice = new GroupMessage("SYSTEM", groupName, "@" + currentUser.getUsername() + " added @" + targetUsername + " to the channel.");
            dbOps.saveMessage(sysNotice);
            server.broadcastGroupMessage(groupName, sysNotice);

            // Notify added user directly if they are online
            ClientHandler targetClient = server.getClient(targetUsername);
            if (targetClient != null) {
                targetClient.sendMessagePacket(NetworkProtocol.CMD_SYSTEM_ALERT, "You were added to channel #" + groupName + " by @" + currentUser.getUsername() + ".");
                targetClient.sendGroupList();
            }
        } else {
            sendMessagePacket(NetworkProtocol.CMD_ERROR, "Could not add @" + targetUsername + " to #" + groupName + ".");
        }
    }

    private void handleGetGroupCreator(String[] parts) throws Exception {
        if (parts.length < 2) return;
        String groupName = parts[1].trim();
        String creator = dbOps.getGroupCreator(groupName);
        sendRawLine(NetworkProtocol.CMD_GROUP_CREATOR + "||" + groupName + "||" + (creator != null ? creator : "SYSTEM"));
    }

    private void handleGetHistory(String[] parts) throws Exception {
        if (parts.length < 3 || currentUser == null) return;
        String target = parts[1].trim();
        boolean isGroup = Boolean.parseBoolean(parts[2].trim()); // Unit I: Type Wrapper parsing

        this.currentActiveChat = target;

        List<Message> history;
        if (isGroup) {
            history = dbOps.getGroupMessageHistory(target, 50);
        } else {
            // Mark any unread messages from this sender as READ
            dbOps.markMessagesAsRead(target, currentUser.getUsername());
            // Notify target sender so their ticks turn blue
            server.notifyStatusUpdate(target, currentUser.getUsername(), "READ");

            history = dbOps.getDirectMessageHistory(currentUser.getUsername(), target, 50);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(NetworkProtocol.CMD_HISTORY_DATA).append("||").append(target).append("||").append(history.size());
        for (Message m : history) {
            String status = (m instanceof TextMessage) ? ((TextMessage) m).getStatus() : "SENT";
            sb.append("||").append(m.getSender())
              .append("::").append(m.getContent())
              .append("::").append(m.getFormattedTime())
              .append("::").append(status)
              .append("::").append(m.getMessageId());
        }
        sendRawLine(sb.toString());
    }

    private void handleMarkRead(String[] parts) throws Exception {
        if (currentUser == null || parts.length < 2) return;
        String sender = parts[1].trim();
        dbOps.markMessagesAsRead(sender, currentUser.getUsername());
        server.notifyStatusUpdate(sender, currentUser.getUsername(), "READ");
    }

    private void handleStatusUpdate(String[] parts) throws Exception {
        if (currentUser == null || parts.length < 2) return;
        String statusStr = parts[1].trim();
        try {
            UserStatus newStatus = UserStatus.valueOf(statusStr);
            currentUser.setStatus(newStatus);
            dbOps.updateUserStatus(currentUser.getUsername(), newStatus.name());
            server.broadcastUserList();
        } catch (IllegalArgumentException ignored) {}
    }

    public void sendUserList() {
        try {
            List<User> users = dbOps.getAllUsers();
            StringBuilder sb = new StringBuilder(NetworkProtocol.CMD_USER_LIST);
            for (User u : users) {
                boolean isOnline = server.isUserOnline(u.getUsername());
                sb.append("||").append(u.getUsername())
                  .append("::").append(u.getFullName())
                  .append("::").append(isOnline ? u.getStatus().name() : "OFFLINE");
            }
            sendRawLine(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendGroupList() {
        try {
            List<String> groups = dbOps.getAllGroups();
            StringBuilder sb = new StringBuilder(NetworkProtocol.CMD_GROUP_LIST);
            for (String g : groups) {
                sb.append("||").append(g);
            }
            sendRawLine(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendRawLine(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    public void sendMessagePacket(String command, String message) {
        sendRawLine(command + "||" + message);
    }

    public User getCurrentUser() {
        return currentUser;
    }

    private void cleanup() {
        isRunning = false;
        if (currentUser != null) {
            try {
                dbOps.updateUserStatus(currentUser.getUsername(), UserStatus.OFFLINE.name());
            } catch (Exception ignored) {}
            server.unregisterClient(currentUser.getUsername());
            server.broadcastSystemMessage(currentUser.getUsername() + " has left the chat.");
            server.broadcastUserList();
        }
        server.onClientDisconnected(this);
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}
