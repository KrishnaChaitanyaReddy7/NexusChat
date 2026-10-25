package nexuschat.network;

import nexuschat.exception.NetworkException;
import nexuschat.model.UserStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Unit III Syllabus:
 * - Multithreading: Creating threads by implementing Runnable interface
 * - Concurrency on the client side: GUI remains responsive while network reads occur on a background thread
 * 
 * Unit IV Syllabus:
 * - java.io package: Socket input and output streams
 * - Event-driven communication via ClientEventListener
 */
public class NexusClient {

    public interface ClientEventListener {
        void onLoginResponse(boolean success, String message, String username, String fullName);
        void onRegisterResponse(boolean success, String message);
        void onDirectMessageReceived(String sender, String recipient, String content, String time, String status, int messageId);
        void onGroupMessageReceived(String sender, String group, String content, String time);
        void onMessageStatusUpdate(String otherParty, String newStatus);
        void onMessageDeleted(int messageId, String target);
        void onSystemAlertReceived(String message);
        void onUserListReceived(String[] userData);
        void onGroupListReceived(String[] groups);
        void onGroupDeleted(String groupName);
        void onGroupCreatorReceived(String groupName, String creator);
        void onHistoryReceived(String target, String[] historyEntries);
        void onConnectionLost(String reason);
    }

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Thread receiverThread;
    private volatile boolean isConnected = false;

    private ClientEventListener eventListener;
    private String currentUsername;

    public void setEventListener(ClientEventListener listener) {
        this.eventListener = listener;
    }

    public synchronized void connect(String host, int port) throws NetworkException {
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            isConnected = true;

            // Unit III: Creating a background thread by implementing Runnable
            receiverThread = new Thread(new MessageReceiverRunnable(), "NexusClient-ReceiverThread");
            receiverThread.start();
        } catch (IOException e) {
            throw new NetworkException("Unable to connect to NexusChat Server at " + host + ":" + port, e);
        }
    }

    /**
     * Unit III: Background Runnable implementation for network reading
     */
    private class MessageReceiverRunnable implements Runnable {
        @Override
        public void run() {
            try {
                String line;
                while (isConnected && (line = reader.readLine()) != null) {
                    processIncomingMessage(line);
                }
            } catch (IOException e) {
                if (isConnected && eventListener != null) {
                    eventListener.onConnectionLost("Connection to server was lost: " + e.getMessage());
                }
            } finally {
                disconnect();
            }
        }
    }

    private void processIncomingMessage(String rawLine) {
        if (rawLine == null || rawLine.trim().isEmpty()) return;
        String[] parts = rawLine.split("\\|\\|");
        String cmd = parts[0];

        if (eventListener == null) return;

        switch (cmd) {
            case NetworkProtocol.CMD_SUCCESS:
                if (parts.length >= 2 && parts[1].startsWith("Login successful")) {
                    String username = (parts.length > 2) ? parts[2] : "";
                    String fullName = (parts.length > 3) ? parts[3] : username;
                    this.currentUsername = username;
                    eventListener.onLoginResponse(true, parts[1], username, fullName);
                } else {
                    eventListener.onRegisterResponse(true, parts.length > 1 ? parts[1] : "Success");
                }
                break;

            case NetworkProtocol.CMD_ERROR:
                String errMsg = parts.length > 1 ? parts[1] : "Unknown error";
                eventListener.onLoginResponse(false, errMsg, null, null);
                eventListener.onRegisterResponse(false, errMsg);
                break;

            case NetworkProtocol.CMD_MSG_DIRECT:
                if (parts.length >= 5) {
                    String status = (parts.length >= 6) ? parts[5] : "SENT";
                    int messageId = (parts.length >= 7) ? Integer.parseInt(parts[6].trim()) : 0;
                    eventListener.onDirectMessageReceived(parts[1], parts[2], parts[3], parts[4], status, messageId);
                }
                break;

            case NetworkProtocol.CMD_MSG_DELETED:
                if (parts.length >= 3) {
                    try {
                        int delId = Integer.parseInt(parts[1].trim());
                        eventListener.onMessageDeleted(delId, parts[2].trim());
                    } catch (NumberFormatException ignored) {}
                }
                break;

            case NetworkProtocol.CMD_MSG_STATUS_UPDATE:
                if (parts.length >= 3) {
                    eventListener.onMessageStatusUpdate(parts[1], parts[2]);
                }
                break;

            case NetworkProtocol.CMD_MSG_GROUP:
                if (parts.length >= 5) {
                    eventListener.onGroupMessageReceived(parts[1], parts[2], parts[3], parts[4]);
                }
                break;

            case NetworkProtocol.CMD_SYSTEM_ALERT:
                if (parts.length >= 2) {
                    eventListener.onSystemAlertReceived(parts[1]);
                }
                break;

            case NetworkProtocol.CMD_USER_LIST:
                // Extract user items (excluding command header)
                String[] users = new String[parts.length - 1];
                System.arraycopy(parts, 1, users, 0, users.length);
                eventListener.onUserListReceived(users);
                break;

            case NetworkProtocol.CMD_GROUP_LIST:
                String[] groups = new String[parts.length - 1];
                System.arraycopy(parts, 1, groups, 0, groups.length);
                eventListener.onGroupListReceived(groups);
                break;

            case NetworkProtocol.CMD_GROUP_DELETED:
                if (parts.length >= 2) {
                    eventListener.onGroupDeleted(parts[1].trim());
                }
                break;

            case NetworkProtocol.CMD_GROUP_CREATOR:
                if (parts.length >= 3) {
                    eventListener.onGroupCreatorReceived(parts[1].trim(), parts[2].trim());
                }
                break;

            case NetworkProtocol.CMD_HISTORY_DATA:
                if (parts.length >= 3) {
                    String target = parts[1];
                    String[] entries = new String[parts.length - 3];
                    System.arraycopy(parts, 3, entries, 0, entries.length);
                    eventListener.onHistoryReceived(target, entries);
                }
                break;
        }
    }

    public void sendRaw(String command) {
        if (writer != null && isConnected) {
            writer.println(command);
        }
    }

    public void login(String username, String password) {
        sendRaw(NetworkProtocol.CMD_LOGIN + "||" + username + "||" + password);
    }

    public void register(String username, String fullName, String password) {
        sendRaw(NetworkProtocol.CMD_REGISTER + "||" + username + "||" + fullName + "||" + password);
    }

    public void sendDirectMessage(String recipient, String message) {
        sendRaw(NetworkProtocol.CMD_MSG_DIRECT + "||" + recipient + "||" + message);
    }

    public void markAsRead(String sender) {
        sendRaw(NetworkProtocol.CMD_MSG_READ + "||" + sender);
    }

    public void deleteMessage(int messageId, String target, boolean isGroup) {
        sendRaw(NetworkProtocol.CMD_DELETE_MSG + "||" + messageId + "||" + target + "||" + isGroup);
    }

    public void sendGroupMessage(String group, String message) {
        sendRaw(NetworkProtocol.CMD_MSG_GROUP + "||" + group + "||" + message);
    }

    public void createGroup(String groupName) {
        sendRaw(NetworkProtocol.CMD_CREATE_GROUP + "||" + groupName);
    }

    public void deleteGroup(String groupName) {
        sendRaw(NetworkProtocol.CMD_DELETE_GROUP + "||" + groupName);
    }

    public void addGroupMember(String groupName, String username) {
        sendRaw(NetworkProtocol.CMD_ADD_GROUP_MEMBER + "||" + groupName + "||" + username);
    }

    public void requestGroupCreator(String groupName) {
        sendRaw(NetworkProtocol.CMD_GET_GROUP_CREATOR + "||" + groupName);
    }

    public void requestUsers() {
        sendRaw(NetworkProtocol.CMD_GET_USERS);
    }

    public void requestGroups() {
        sendRaw(NetworkProtocol.CMD_GET_GROUPS);
    }

    public void requestHistory(String target, boolean isGroup) {
        sendRaw(NetworkProtocol.CMD_GET_HISTORY + "||" + target + "||" + isGroup);
    }

    public void updateStatus(UserStatus status) {
        sendRaw(NetworkProtocol.CMD_STATUS_UPDATE + "||" + status.name());
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void disconnect() {
        isConnected = false;
        try {
            if (writer != null) {
                writer.println(NetworkProtocol.CMD_LOGOUT);
                writer.close();
            }
            if (reader != null) reader.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}
