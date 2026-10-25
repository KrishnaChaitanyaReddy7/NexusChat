package nexuschat.ui;

import nexuschat.dao.DatabaseManager;
import nexuschat.dao.DatabaseOperations;
import nexuschat.exception.NetworkException;
import nexuschat.model.Message;
import nexuschat.network.NexusClient;
import nexuschat.service.AdvancedChatService;
import nexuschat.service.ChatServiceImpl;
import nexuschat.util.HistorySearchUtil;
import nexuschat.util.NexusConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;

/**
 * Unit IV Syllabus:
 * - Layout Manager: CardLayout for switching between Login and Chat views
 * - Event Handling: Delegation event model, ActionEvent, WindowEvent, WindowAdapter
 * 
 * Unit V Syllabus:
 * - Swing Components: JFrame, JMenuBar, JMenu, JMenuItem, JSeparator, JFileChooser, JOptionPane
 * - Clean, non-technical, user-friendly desktop experience
 */
public class NexusChatApp extends JFrame implements LoginPanel.LoginListener, NexusClient.ClientEventListener {

    private static final String CARD_LOGIN = "CARD_LOGIN";
    private static final String CARD_CHAT = "CARD_CHAT";

    private final CardLayout cardLayout;
    private final JPanel cardsContainer;

    private final LoginPanel loginPanel;
    private final ChatDashboardPanel chatDashboardPanel;

    private final NexusClient client;
    private final DatabaseOperations dbOps;
    private final AdvancedChatService chatService;

    // Menu components
    private JMenuItem exportHistoryMenuItem;
    private JMenuItem searchMessagesMenuItem;
    private JMenuItem viewUsersMenuItem;
    private JMenuItem logoutMenuItem;

    private String[] cachedUserData = new String[0];

    public NexusChatApp() {
        super(NexusConstants.APP_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(780, 560);
        setMinimumSize(new Dimension(640, 440));
        setLocationRelativeTo(null);

        // Core client services
        this.client = new NexusClient();
        this.client.setEventListener(this);

        this.dbOps = new DatabaseManager();
        this.chatService = new ChatServiceImpl(dbOps);

        // Unit IV: CardLayout to switch smoothly between Login and Main Chat
        cardLayout = new CardLayout();
        cardsContainer = new JPanel(cardLayout);

        loginPanel = new LoginPanel();
        loginPanel.setLoginListener(this);

        chatDashboardPanel = new ChatDashboardPanel(client);

        cardsContainer.add(loginPanel, CARD_LOGIN);
        cardsContainer.add(chatDashboardPanel, CARD_CHAT);

        add(cardsContainer, BorderLayout.CENTER);

        // Build clean user-friendly menu
        initializeSimpleMenuBar();

        // Unit IV: WindowAdapter to cleanly disconnect on close without killing server or other windows
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClose();
            }
        });

        // Show login card
        cardLayout.show(cardsContainer, CARD_LOGIN);
    }

    private void handleWindowClose() {
        if (client.isConnected()) {
            client.disconnect();
        }
        dispose();

        // Check if any other visible NexusChat window is still open in this JVM process
        boolean hasOtherWindows = false;
        for (Frame frame : Frame.getFrames()) {
            if (frame != this && frame.isDisplayable()) {
                hasOtherWindows = true;
                break;
            }
        }
        if (!hasOtherWindows) {
            System.exit(0);
        }
    }

    /**
     * Clean, simple, user-friendly menu without development clutter
     */
    private void initializeSimpleMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // 1. Chat Menu
        JMenu chatMenu = new JMenu("Chat");
        chatMenu.setMnemonic('C');

        exportHistoryMenuItem = new JMenuItem("Export Conversation to File...");
        exportHistoryMenuItem.setEnabled(false);
        exportHistoryMenuItem.addActionListener(e -> handleExportChat());

        searchMessagesMenuItem = new JMenuItem("Search in Conversation...");
        searchMessagesMenuItem.setEnabled(false);
        searchMessagesMenuItem.addActionListener(e -> handleSearchMessages());

        logoutMenuItem = new JMenuItem("Log Out");
        logoutMenuItem.setEnabled(false);
        logoutMenuItem.addActionListener(e -> {
            client.disconnect();
            cardLayout.show(cardsContainer, CARD_LOGIN);
            updateMenuState(false);
            loginPanel.clearPassword();
            loginPanel.setStatusSuccess("Logged out successfully.");
        });

        JMenuItem newWindowItem = new JMenuItem("Open New Chat Window...");
        newWindowItem.addActionListener(e -> {
            NexusChatApp newApp = new NexusChatApp();
            newApp.setVisible(true);
        });

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> handleWindowClose());

        chatMenu.add(exportHistoryMenuItem);
        chatMenu.add(searchMessagesMenuItem);
        chatMenu.add(new JSeparator());
        chatMenu.add(newWindowItem);
        chatMenu.add(logoutMenuItem);
        chatMenu.add(exitItem);

        // 2. Contacts Menu
        JMenu contactsMenu = new JMenu("Contacts");
        contactsMenu.setMnemonic('O');

        viewUsersMenuItem = new JMenuItem("All Users Directory (Table)...");
        viewUsersMenuItem.setEnabled(false);
        viewUsersMenuItem.addActionListener(e -> {
            UserDirectoryDialog dialog = new UserDirectoryDialog(NexusChatApp.this, cachedUserData);
            dialog.setVisible(true);
        });

        JMenuItem newChanItem = new JMenuItem("Create Channel...");
        newChanItem.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Enter new channel name:", "Create Channel", JOptionPane.PLAIN_MESSAGE);
            if (name != null && !name.trim().isEmpty()) {
                client.createGroup(name.trim());
            }
        });

        contactsMenu.add(viewUsersMenuItem);
        contactsMenu.add(newChanItem);

        // 3. Help Menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('H');

        JMenuItem aboutItem = new JMenuItem("About NexusChat");
        aboutItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    NexusConstants.APP_NAME + " v" + NexusConstants.APP_VERSION + "\n"
                            + "A simple, clean multi-user messaging application.\n"
                            + "Built with Java Swing, Multithreading, Sockets, and JDBC.",
                    "About " + NexusConstants.APP_NAME,
                    JOptionPane.INFORMATION_MESSAGE);
        });

        helpMenu.add(aboutItem);

        menuBar.add(chatMenu);
        menuBar.add(contactsMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void updateMenuState(boolean loggedIn) {
        exportHistoryMenuItem.setEnabled(loggedIn);
        searchMessagesMenuItem.setEnabled(loggedIn);
        viewUsersMenuItem.setEnabled(loggedIn);
        logoutMenuItem.setEnabled(loggedIn);
    }

    /**
     * Demonstrates Unit IV: java.io Package file export
     */
    private void handleExportChat() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(chatDashboardPanel.getCurrentTarget() + "_conversation.txt"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                chatService.exportChatHistory(
                        chatDashboardPanel.getCurrentTarget(),
                        file.getAbsolutePath(),
                        chatDashboardPanel.isTargetGroup()
                );
                JOptionPane.showMessageDialog(this,
                        "Conversation exported successfully to:\n" + file.getAbsolutePath(),
                        "Export Successful",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not export conversation: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Demonstrates Unit I: Recursion via HistorySearchUtil
     */
    private void handleSearchMessages() {
        String keyword = JOptionPane.showInputDialog(this, "Search messages for keyword:", "Find Messages", JOptionPane.QUESTION_MESSAGE);
        if (keyword != null && !keyword.trim().isEmpty()) {
            try {
                List<Message> history = chatService.retrieveHistory(chatDashboardPanel.getCurrentTarget(), chatDashboardPanel.isTargetGroup());
                List<Message> matches = HistorySearchUtil.searchByKeyword(history, keyword.trim());

                if (matches.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No messages matched: \"" + keyword + "\"", "Search Results", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("Found ").append(matches.size()).append(" matching message(s):\n\n");
                for (Message m : matches) {
                    sb.append(m.getFormattedDisplay()).append("\n");
                }

                JTextArea area = new JTextArea(sb.toString(), 14, 45);
                area.setEditable(false);
                JOptionPane.showMessageDialog(this, new JScrollPane(area), "Search Results for \"" + keyword + "\"", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error searching: " + ex.getMessage());
            }
        }
    }

    // --- LoginPanel.LoginListener Callbacks ---

    @Override
    public void onLoginSubmitted(String host, int port, String username, String password) {
        loginPanel.setStatusSuccess("Connecting...");
        // Run network connection in background thread so GUI never freezes
        new Thread(() -> {
            try {
                // If connecting locally, ensure the local server is running
                if (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1")) {
                    nexuschat.network.ServerBootstrap.ensureServerRunning(port);
                }

                if (!client.isConnected()) {
                    client.connect(host, port);
                }
                client.login(username, password);
            } catch (NetworkException e) {
                SwingUtilities.invokeLater(() -> {
                    loginPanel.setStatusError("Could not connect: " + e.getMessage());
                });
            }
        }, "Nexus-LoginWorker").start();
    }

    @Override
    public void onRegisterSubmitted(String host, int port, String username, String fullName, String password) {
        loginPanel.setStatusSuccess("Creating account...");
        // Run network registration in background thread so GUI never freezes
        new Thread(() -> {
            try {
                // If connecting locally, ensure the local server is running
                if (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1")) {
                    nexuschat.network.ServerBootstrap.ensureServerRunning(port);
                }

                if (!client.isConnected()) {
                    client.connect(host, port);
                }
                client.register(username, fullName, password);
            } catch (NetworkException e) {
                SwingUtilities.invokeLater(() -> {
                    loginPanel.setStatusError("Could not connect: " + e.getMessage());
                });
            }
        }, "Nexus-RegisterWorker").start();
    }

    // --- NexusClient.ClientEventListener Callbacks ---

    @Override
    public void onLoginResponse(boolean success, String message, String username, String fullName) {
        SwingUtilities.invokeLater(() -> {
            if (success) {
                updateMenuState(true);
                chatDashboardPanel.initializeSession(username, fullName);
                cardLayout.show(cardsContainer, CARD_CHAT);
            } else {
                loginPanel.setStatusError(message);
            }
        });
    }

    @Override
    public void onRegisterResponse(boolean success, String message) {
        SwingUtilities.invokeLater(() -> {
            if (success) {
                loginPanel.setStatusSuccess(message);
            } else {
                loginPanel.setStatusError(message);
            }
        });
    }

    @Override
    public void onDirectMessageReceived(String sender, String recipient, String content, String time, String status, int messageId) {
        SwingUtilities.invokeLater(() -> {
            chatDashboardPanel.displayDirectMessage(sender, recipient, content, time, status, messageId);
        });
    }

    @Override
    public void onMessageStatusUpdate(String otherParty, String newStatus) {
        SwingUtilities.invokeLater(() -> {
            chatDashboardPanel.updateMessageStatus(otherParty, newStatus);
        });
    }

    @Override
    public void onMessageDeleted(int messageId, String target) {
        SwingUtilities.invokeLater(() -> {
            chatDashboardPanel.handleMessageDeleted(messageId, target);
        });
    }

    @Override
    public void onGroupMessageReceived(String sender, String group, String content, String time) {
        SwingUtilities.invokeLater(() -> {
            chatDashboardPanel.displayGroupMessage(sender, group, content, time);
        });
    }

    @Override
    public void onSystemAlertReceived(String message) {
        SwingUtilities.invokeLater(() -> {
            chatDashboardPanel.appendNotice(message);
        });
    }

    @Override
    public void onUserListReceived(String[] userData) {
        this.cachedUserData = userData;
        SwingUtilities.invokeLater(() -> {
            chatDashboardPanel.updateUserList(userData);
        });
    }

    @Override
    public void onGroupListReceived(String[] groups) {
        SwingUtilities.invokeLater(() -> {
            chatDashboardPanel.updateGroupList(groups);
        });
    }

    @Override
    public void onGroupDeleted(String groupName) {
        SwingUtilities.invokeLater(() -> {
            chatDashboardPanel.handleGroupDeleted(groupName);
        });
    }

    @Override
    public void onGroupCreatorReceived(String groupName, String creator) {
        SwingUtilities.invokeLater(() -> {
            chatDashboardPanel.handleGroupCreator(groupName, creator);
        });
    }

    @Override
    public void onHistoryReceived(String target, String[] historyEntries) {
        SwingUtilities.invokeLater(() -> {
            chatDashboardPanel.displayHistory(target, historyEntries);
        });
    }

    @Override
    public void onConnectionLost(String reason) {
        SwingUtilities.invokeLater(() -> {
            updateMenuState(false);
            cardLayout.show(cardsContainer, CARD_LOGIN);
            loginPanel.setStatusError(reason);
        });
    }

    public static void main(String[] args) {
        // Pre-warm local server in background for instant login
        new Thread(() -> {
            nexuschat.network.ServerBootstrap.ensureServerRunning(NexusConstants.DEFAULT_PORT);
        }, "Nexus-PreWarmServer").start();

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            NexusChatApp app = new NexusChatApp();
            app.setVisible(true);
        });
    }
}
