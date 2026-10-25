package nexuschat.network;

import nexuschat.dao.DatabaseManager;
import nexuschat.dao.DatabaseOperations;
import nexuschat.model.GroupMessage;
import nexuschat.model.Message;
import nexuschat.model.SystemMessage;
import nexuschat.model.TextMessage;
import nexuschat.util.NexusConstants;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Unit II Syllabus:
 * - Implementing interfaces (NetworkProtocol)
 * 
 * Unit III Syllabus:
 * - Multithreading & Concurrency
 * - Thread lifecycle and ThreadGroups
 * - Daemon Threads: Background heartbeat/maintenance thread
 * - Thread Synchronization: synchronized methods and blocks for concurrent client safety
 * - Inter-thread communication using SynchronizedMessageBuffer
 * 
 * Unit IV Syllabus:
 * - Collections Framework: Map (HashMap), Set (HashSet), List (ArrayList)
 * - java.io.IOException handling
 * 
 * Unit V Syllabus:
 * - JDBC integration via DatabaseOperations
 */
public class NexusServer implements NetworkProtocol {

    private final int port;
    private final DatabaseOperations dbOps;

    // Unit IV: Collections Framework - Map to store active client handlers by username
    private final Map<String, ClientHandler> activeClients = new HashMap<>();
    
    // Unit IV: Collections Framework - Set to track active online usernames
    private final Set<String> onlineUsers = new HashSet<>();

    // Unit III: ThreadGroup to manage all client handler threads collectively
    private final ThreadGroup clientThreadGroup = new ThreadGroup("NexusClientHandlersGroup");

    // Unit III: Synchronized message buffer for asynchronous broadcast distribution
    private final SynchronizedMessageBuffer broadcastBuffer = new SynchronizedMessageBuffer(100);

    // Track all connected client handlers across their lifecycle
    private final Set<ClientHandler> connectedHandlers = Collections.synchronizedSet(new HashSet<>());
    private volatile boolean hasHadClients = false;
    private volatile boolean autoCloseWhenEmpty = true;
    private volatile boolean exitOnShutdown = false;
    private Thread shutdownCountdownThread = null;
    private long serverStartTime = 0;

    private ServerSocket serverSocket;
    private volatile boolean isRunning = false;

    public void setAutoCloseWhenEmpty(boolean autoCloseWhenEmpty) {
        this.autoCloseWhenEmpty = autoCloseWhenEmpty;
    }

    public void setExitOnShutdown(boolean exitOnShutdown) {
        this.exitOnShutdown = exitOnShutdown;
    }

    public NexusServer(int port) {
        this(port, new DatabaseManager());
    }

    public NexusServer(int port, DatabaseOperations dbOps) {
        this.port = port;
        this.dbOps = dbOps;
    }

    @Override
    public void start() {
        try {
            serverStartTime = System.currentTimeMillis();

            // Unit V: Initialize JDBC Database tables
            System.out.println("[NexusServer] Initializing database layer...");
            dbOps.initializeDatabase();

            // Start ServerSocket
            serverSocket = new ServerSocket(port);
            isRunning = true;
            System.out.println("=================================================");
            System.out.println("   NexusChat Server v" + NexusConstants.APP_VERSION + " Started");
            System.out.println("   Listening on Port: " + port);
            System.out.println("=================================================");

            // Unit III: Start Daemon Thread for background maintenance
            startDaemonMonitor();

            // Unit III: Start Asynchronous Broadcast Dispatcher Thread
            startBroadcastDispatcher();

            // Main server listener loop: accepts incoming client socket connections
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[NexusServer] Incoming connection accepted from: " + clientSocket.getRemoteSocketAddress());

                // Unit III: Create thread extending Thread with ThreadGroup and start execution
                ClientHandler handler = new ClientHandler(clientThreadGroup, clientSocket, this, dbOps);
                connectedHandlers.add(handler);
                hasHadClients = true;
                cancelShutdownCountdown();
                handler.start(); // Moves thread to RUNNABLE state
            }
        } catch (IOException e) {
            if (isRunning) {
                System.err.println("[NexusServer] Server exception: " + e.getMessage());
            }
        } catch (Exception ex) {
            System.err.println("[NexusServer] Fatal startup error: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            stop();
        }
    }

    /**
     * Unit III: Daemon Thread demonstration
     * Daemon threads run in background and automatically terminate when all user threads finish.
     */
    private void startDaemonMonitor() {
        Thread daemonThread = new Thread(() -> {
            while (isRunning) {
                try {
                    Thread.sleep(15000); // Check every 15 seconds
                    int activeThreads = clientThreadGroup.activeCount();
                    System.out.println("[Daemon Monitor] Active client threads: " + activeThreads 
                            + " | Online registered users: " + getOnlineUserCount());

                    // If server was started but no client connected within 60s, auto-terminate
                    if (autoCloseWhenEmpty && !hasHadClients && (System.currentTimeMillis() - serverStartTime > 60000)) {
                        System.out.println("[NexusServer] No clients connected within 60s of startup. Auto-shutting down.");
                        stop();
                        if (exitOnShutdown) {
                            System.exit(0);
                        }
                        break;
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "Nexus-Daemon-Heartbeat");

        // Mark thread as daemon and assign lowest priority
        daemonThread.setDaemon(true);
        daemonThread.setPriority(Thread.MIN_PRIORITY);
        daemonThread.start();
    }

    /**
     * Unit III: Worker Thread consuming messages from SynchronizedMessageBuffer
     */
    private void startBroadcastDispatcher() {
        Thread dispatcherThread = new Thread(() -> {
            while (isRunning) {
                try {
                    // Inter-thread communication: consumer waits if buffer is empty
                    Message message = broadcastBuffer.consume();
                    deliverBroadcast(message);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "Nexus-Broadcast-Dispatcher");

        dispatcherThread.setDaemon(true);
        dispatcherThread.setPriority(Thread.NORM_PRIORITY + 1);
        dispatcherThread.start();
    }

    private void deliverBroadcast(Message message) {
        String payload;
        if (message instanceof GroupMessage) {
            GroupMessage gm = (GroupMessage) message;
            payload = CMD_MSG_GROUP + "||" + gm.getSender() + "||" + gm.getGroupName() + "||" + gm.getContent() + "||" + gm.getFormattedTime();
        } else if (message instanceof SystemMessage) {
            payload = CMD_SYSTEM_ALERT + "||" + message.getContent();
        } else {
            payload = CMD_SYSTEM_ALERT + "||" + message.getFormattedDisplay();
        }

        // Thread-safe iteration over active clients
        synchronized (activeClients) {
            for (ClientHandler client : activeClients.values()) {
                client.sendRawLine(payload);
            }
        }
    }

    // Unit III: Synchronized method to register authenticated client
    public synchronized void registerClient(String username, ClientHandler handler) {
        activeClients.put(username.toLowerCase(), handler);
        onlineUsers.add(username.toLowerCase());
        System.out.println("[NexusServer] User registered: @" + username + " (Total online: " + onlineUsers.size() + ")");
    }

    // Unit III: Synchronized method to unregister disconnected client
    public synchronized void unregisterClient(String username) {
        if (username != null) {
            activeClients.remove(username.toLowerCase());
            onlineUsers.remove(username.toLowerCase());
            System.out.println("[NexusServer] User unregistered: @" + username);
        }
    }

    public synchronized boolean isUserOnline(String username) {
        return username != null && onlineUsers.contains(username.toLowerCase());
    }

    public synchronized int getOnlineUserCount() {
        return onlineUsers.size();
    }

    public synchronized ClientHandler getClient(String username) {
        if (username == null) return null;
        return activeClients.get(username.toLowerCase());
    }

    public synchronized void notifyStatusUpdate(String targetUser, String otherParty, String newStatus) {
        ClientHandler handler = getClient(targetUser);
        if (handler != null) {
            handler.sendRawLine(CMD_MSG_STATUS_UPDATE + "||" + otherParty + "||" + newStatus);
        }
    }

    // Synchronized direct message delivery
    public synchronized boolean sendDirectMessage(String recipient, TextMessage message) {
        ClientHandler target = activeClients.get(recipient.toLowerCase());
        if (target != null) {
            target.sendRawLine(CMD_MSG_DIRECT + "||" + message.getSender() + "||" + message.getRecipient() + "||" + message.getContent() + "||" + message.getFormattedTime() + "||" + message.getStatus() + "||" + message.getMessageId());
            return true;
        }
        return false;
    }

    public synchronized void notifyMessageDeleted(String targetUser, int messageId, String chatTarget) {
        ClientHandler handler = getClient(targetUser);
        if (handler != null) {
            handler.sendRawLine(CMD_MSG_DELETED + "||" + messageId + "||" + chatTarget);
        }
    }

    public synchronized void broadcastGroupMessageDeleted(String groupName, int messageId) {
        for (ClientHandler client : activeClients.values()) {
            client.sendRawLine(CMD_MSG_DELETED + "||" + messageId + "||" + groupName);
        }
    }

    // Queue group message in the synchronized buffer
    public void broadcastGroupMessage(String groupName, GroupMessage message) {
        try {
            broadcastBuffer.produce(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Broadcast system notice
    public void broadcastSystemMessage(String content) {
        try {
            broadcastBuffer.produce(new SystemMessage(content));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void broadcastUserList() {
        for (ClientHandler client : activeClients.values()) {
            client.sendUserList();
        }
    }

    public synchronized void broadcastGroupList() {
        for (ClientHandler client : activeClients.values()) {
            client.sendGroupList();
        }
    }

    public synchronized void broadcastGroupDeleted(String groupName) {
        for (ClientHandler client : activeClients.values()) {
            client.sendRawLine(CMD_GROUP_DELETED + "||" + groupName);
        }
    }

    /**
     * Called whenever any client disconnects or closes their window.
     * Checks if all clients are closed; if so, triggers auto-shutdown.
     */
    public synchronized void onClientDisconnected(ClientHandler handler) {
        connectedHandlers.remove(handler);
        System.out.println("[NexusServer] Client disconnected. Remaining active connections: " + connectedHandlers.size());

        if (autoCloseWhenEmpty && hasHadClients && connectedHandlers.isEmpty()) {
            scheduleAutoShutdown();
        }
    }

    private synchronized void cancelShutdownCountdown() {
        if (shutdownCountdownThread != null && shutdownCountdownThread.isAlive()) {
            shutdownCountdownThread.interrupt();
            shutdownCountdownThread = null;
        }
    }

    /**
     * When all clients are closed, waits a 5-second grace period.
     * If no new client connects within 5 seconds, cleanly shuts down the server.
     */
    private synchronized void scheduleAutoShutdown() {
        cancelShutdownCountdown();
        shutdownCountdownThread = new Thread(() -> {
            try {
                System.out.println("[NexusServer] All clients are closed. Server will shut down in 5 seconds unless a client reconnects...");
                Thread.sleep(5000);
                synchronized (NexusServer.this) {
                    if (connectedHandlers.isEmpty()) {
                        System.out.println("[NexusServer] All clients closed. Automatically shutting down server process.");
                        stop();
                        if (exitOnShutdown) {
                            System.exit(0);
                        }
                    }
                }
            } catch (InterruptedException ignored) {
                System.out.println("[NexusServer] Auto-shutdown cancelled (client reconnected).");
            }
        }, "Nexus-AutoShutdown-Timer");
        shutdownCountdownThread.setDaemon(true);
        shutdownCountdownThread.start();
    }

    @Override
    public void stop() {
        isRunning = false;
        cancelShutdownCountdown();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}

        // Interrupt all client threads in ThreadGroup
        clientThreadGroup.interrupt();
        System.out.println("[NexusServer] Server stopped.");
    }

    public static void main(String[] args) {
        int port = NexusConstants.DEFAULT_PORT;
        if (args.length > 0) {
            try {
                // Unit I: Type Wrapper parsing
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port provided. Using default: " + port);
            }
        }
        NexusServer server = new NexusServer(port);
        server.setAutoCloseWhenEmpty(true);
        server.setExitOnShutdown(true);
        server.start();
    }
}
