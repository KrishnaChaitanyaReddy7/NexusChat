package nexuschat.ui;

import nexuschat.model.UserStatus;
import nexuschat.network.NexusClient;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit IV Syllabus:
 * - Layout manager types: BorderLayout, FlowLayout, GridLayout
 * - Event Handling: ActionEvent, MouseEvent, KeyEvent, MouseAdapter, KeyAdapter
 * 
 * Unit V Syllabus:
 * - Swing Components: JLabel, JTextField, JButton, JCheckBox,
 *   JComboBox, JList, JTree, JPopupMenu, JMenuItem, JSplitPane, JScrollPane, JTextPane
 * 
 * WhatsApp-Style UI:
 * - Each message displayed inside its own chat box/bubble (Green for outgoing, White for incoming)
 * - Single tick (✓), Double grey tick (✓✓), Double blue tick (✓✓)
 * - Option to delete messages via bubble [✕] link or right-click menu
 */
public class ChatDashboardPanel extends JPanel {

    private final NexusClient client;
    private String currentUsername = "";
    private String currentTarget = "General";
    private boolean isTargetGroup = true;

    // Header Components
    private JLabel userBadgeLabel;
    private JComboBox<UserStatus> statusComboBox;
    private JLabel currentChatTitleLabel;
    private JButton addMemberBtn;
    private JButton deleteChannelBtn;
    private final Map<String, String> channelCreators = new HashMap<>();

    // Left Sidebar: Channels & Contacts
    private JTree channelTree;
    private DefaultMutableTreeNode rootTreeNode;
    private DefaultMutableTreeNode channelsGroupNode;
    private DefaultTreeModel treeModel;

    private JList<String> contactList;
    private DefaultListModel<String> contactListModel;

    // Cached raw contact data & unread counts per contact
    private String[] lastRawUserData = new String[0];
    private final Map<String, Integer> unreadCounts = new HashMap<>();

    // Center Chat Window with HTML Rich-Text support
    private JTextPane chatPane;
    private JScrollPane chatScrollPane;

    // In-memory messages for current active chat
    private static class ChatItem {
        int messageId;
        String sender;
        String content;
        String time;
        String status; // SENT, DELIVERED, READ
        boolean isMe;
        boolean isSystem;

        ChatItem(int messageId, String sender, String content, String time, String status, boolean isMe, boolean isSystem) {
            this.messageId = messageId;
            this.sender = sender;
            this.content = content;
            this.time = time;
            this.status = status;
            this.isMe = isMe;
            this.isSystem = isSystem;
        }
    }
    private final List<ChatItem> currentChatItems = new ArrayList<>();

    // Bottom Input Bar
    private JTextField messageInputField;
    private JButton sendButton;
    private JCheckBox autoScrollBox;

    // Context Popup Menu (Unit V)
    private JPopupMenu contextPopupMenu;

    public ChatDashboardPanel(NexusClient client) {
        this.client = client;
        setLayout(new BorderLayout(0, 0));

        initializeHeaderBar();
        initializeCenterWorkspace();
        initializeBottomInputBar();
        initializePopupMenu();
    }

    public void initializeSession(String username, String fullName) {
        this.currentUsername = username;
        this.currentTarget = "General";
        this.isTargetGroup = true;
        this.unreadCounts.clear();
        this.currentChatItems.clear();
        this.channelCreators.clear();

        userBadgeLabel.setText("👤 " + fullName + " (@" + username + ")");
        currentChatTitleLabel.setText("💬 #General (Public Channel)");
        if (addMemberBtn != null) addMemberBtn.setVisible(true);
        if (deleteChannelBtn != null) {
            deleteChannelBtn.setVisible(true);
            deleteChannelBtn.setEnabled(false);
            deleteChannelBtn.setToolTipText("Default channels cannot be deleted");
        }
        renderHtmlChat();
        appendNotice("Welcome to NexusChat, " + fullName + "!");

        // Refresh contact list display to ensure current user is excluded
        refreshContactListDisplay();

        // Request initial user list, group list, and channel history
        client.requestUsers();
        client.requestGroups();
        client.requestGroupCreator("General");
        client.requestHistory("General", true);
    }

    /**
     * Top Bar: Displays User Profile, Status Selector, and Current Active Conversation
     */
    private void initializeHeaderBar() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(245, 247, 250));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(215, 220, 228)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        // Left profile section
        JPanel leftProfile = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftProfile.setOpaque(false);

        userBadgeLabel = new JLabel("👤 Guest");
        userBadgeLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        userBadgeLabel.setForeground(new Color(30, 40, 60));

        // Unit V: JComboBox for User Status
        statusComboBox = new JComboBox<>(new UserStatus[]{UserStatus.ONLINE, UserStatus.AWAY, UserStatus.BUSY});
        statusComboBox.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusComboBox.addActionListener(e -> {
            UserStatus selected = (UserStatus) statusComboBox.getSelectedItem();
            if (selected != null) {
                client.updateStatus(selected);
            }
        });

        leftProfile.add(userBadgeLabel);
        leftProfile.add(new JLabel("|"));
        leftProfile.add(new JLabel("Status:"));
        leftProfile.add(statusComboBox);

        // Right conversation title and channel actions
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);

        currentChatTitleLabel = new JLabel("💬 #General (Public Channel)");
        currentChatTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        currentChatTitleLabel.setForeground(new Color(25, 75, 140));

        addMemberBtn = new JButton("+ Add User");
        addMemberBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        addMemberBtn.setToolTipText("Add a user to this channel by username");
        addMemberBtn.setMargin(new Insets(2, 8, 2, 8));
        addMemberBtn.setFocusable(false);
        addMemberBtn.addActionListener(e -> promptAddUserToChannel());

        deleteChannelBtn = new JButton("Delete Channel");
        deleteChannelBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        deleteChannelBtn.setForeground(new Color(180, 40, 40));
        deleteChannelBtn.setToolTipText("Delete this channel (Owner only)");
        deleteChannelBtn.setMargin(new Insets(2, 8, 2, 8));
        deleteChannelBtn.setFocusable(false);
        deleteChannelBtn.setEnabled(false); // Disabled by default on General
        deleteChannelBtn.addActionListener(e -> promptDeleteChannel());

        rightPanel.add(currentChatTitleLabel);
        rightPanel.add(addMemberBtn);
        rightPanel.add(deleteChannelBtn);

        headerPanel.add(leftProfile, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);
    }

    /**
     * Center Workspace: Left Sidebar (Channels + Contacts) and Center Chat History
     */
    private void initializeCenterWorkspace() {
        // --- Left Sidebar ---
        JPanel sidebarPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        sidebarPanel.setPreferredSize(new Dimension(220, 400));
        sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(215, 220, 228)));

        // 1. Channels Panel with JTree
        JPanel channelsContainer = new JPanel(new BorderLayout());
        JPanel channelHeader = new JPanel(new BorderLayout());
        channelHeader.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JLabel chanLabel = new JLabel("Channels");
        chanLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        JButton addChanBtn = new JButton("+");
        addChanBtn.setToolTipText("Create new channel");
        addChanBtn.setMargin(new Insets(1, 6, 1, 6));
        addChanBtn.addActionListener(e -> promptCreateChannel());
        channelHeader.add(chanLabel, BorderLayout.WEST);
        channelHeader.add(addChanBtn, BorderLayout.EAST);

        rootTreeNode = new DefaultMutableTreeNode("Root");
        channelsGroupNode = new DefaultMutableTreeNode("Channels");
        channelsGroupNode.add(new DefaultMutableTreeNode("General"));
        channelsGroupNode.add(new DefaultMutableTreeNode("Developers"));
        rootTreeNode.add(channelsGroupNode);

        treeModel = new DefaultTreeModel(rootTreeNode);
        channelTree = new JTree(treeModel);
        channelTree.setRootVisible(false);
        channelTree.setFont(new Font("SansSerif", Font.PLAIN, 12));

        // Popup menu for channel tree actions
        JPopupMenu channelTreeMenu = new JPopupMenu();
        JMenuItem openChannelItem = new JMenuItem("Open Channel");
        JMenuItem addMemberItem = new JMenuItem("+ Add User by Username...");
        JMenuItem deleteChannelItem = new JMenuItem("Delete Channel (Owner only)");

        channelTreeMenu.add(openChannelItem);
        channelTreeMenu.addSeparator();
        channelTreeMenu.add(addMemberItem);
        channelTreeMenu.add(deleteChannelItem);

        openChannelItem.addActionListener(e -> {
            TreePath path = channelTree.getSelectionPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.isLeaf() && node != channelsGroupNode) {
                    switchToTarget(node.getUserObject().toString(), true);
                }
            }
        });
        addMemberItem.addActionListener(e -> promptAddUserToChannel());
        deleteChannelItem.addActionListener(e -> promptDeleteChannel());

        // Unit IV: MouseAdapter on JTree to switch channel or open context menu
        channelTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path = channelTree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (node.isLeaf() && node != channelsGroupNode) {
                        String chan = node.getUserObject().toString();
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            switchToTarget(chan, true);
                        } else if (SwingUtilities.isRightMouseButton(e)) {
                            channelTree.setSelectionPath(path);
                            switchToTarget(chan, true);
                            channelTreeMenu.show(channelTree, e.getX(), e.getY());
                        }
                    }
                }
            }
        });

        channelsContainer.add(channelHeader, BorderLayout.NORTH);
        channelsContainer.add(new JScrollPane(channelTree), BorderLayout.CENTER);
        sidebarPanel.add(channelsContainer);

        // 2. Direct Contacts Panel with JList
        JPanel contactsContainer = new JPanel(new BorderLayout());
        JPanel contactsHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JLabel contLabel = new JLabel("Contacts (Direct Message)");
        contLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        contactsHeader.add(contLabel);

        contactListModel = new DefaultListModel<>();
        contactList = new JList<>(contactListModel);
        contactList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        contactList.setFont(new Font("SansSerif", Font.PLAIN, 12));

        // Unit IV: MouseAdapter on JList - Click to open 1-on-1 private chat (WhatsApp style)
        contactList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = contactList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    String selected = contactListModel.getElementAt(index);
                    String targetUser = extractUsername(selected);

                    if (!targetUser.isEmpty() && !targetUser.equalsIgnoreCase(currentUsername)) {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            switchToTarget(targetUser, false);
                        }
                    }
                    if (SwingUtilities.isRightMouseButton(e)) {
                        contactList.setSelectedIndex(index);
                        contextPopupMenu.show(contactList, e.getX(), e.getY());
                    }
                }
            }
        });

        contactsContainer.add(contactsHeader, BorderLayout.NORTH);
        contactsContainer.add(new JScrollPane(contactList), BorderLayout.CENTER);
        sidebarPanel.add(contactsContainer);

        // --- Center Message Area: WhatsApp-style Bubbles in HTML JTextPane ---
        chatPane = new JTextPane();
        chatPane.setContentType("text/html");
        chatPane.setEditable(false);
        chatPane.setBackground(new Color(239, 234, 226)); // Classic WhatsApp chat background tone
        chatPane.setMargin(new Insets(10, 14, 10, 14));

        // Unit IV: HyperlinkListener for delete action clicks on [✕]
        chatPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                String desc = e.getDescription();
                if (desc != null && desc.startsWith("delete:")) {
                    try {
                        int msgId = Integer.parseInt(desc.substring(7));
                        confirmAndDeleteMessage(msgId);
                    } catch (NumberFormatException ignored) {}
                }
            }
        });

        chatScrollPane = new JScrollPane(chatPane);
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebarPanel, chatScrollPane);
        splitPane.setDividerLocation(220);
        splitPane.setDividerSize(4);
        add(splitPane, BorderLayout.CENTER);
    }

    /**
     * Bottom Input Bar: Message Text Field and Send Button
     */
    private void initializeBottomInputBar() {
        JPanel bottomBar = new JPanel(new BorderLayout(8, 0));
        bottomBar.setBackground(Color.WHITE);
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(215, 220, 228)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        messageInputField = new JTextField();
        messageInputField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        messageInputField.setToolTipText("Type a message and press Enter");

        // KeyAdapter: Press Enter to send
        messageInputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleSendMessage();
                }
            }
        });

        sendButton = new JButton("Send");
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        sendButton.setPreferredSize(new Dimension(85, 32));
        sendButton.addActionListener(e -> handleSendMessage());

        autoScrollBox = new JCheckBox("Auto-scroll", true);
        autoScrollBox.setFont(new Font("SansSerif", Font.PLAIN, 11));
        autoScrollBox.setOpaque(false);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controls.setOpaque(false);
        controls.add(autoScrollBox);
        controls.add(sendButton);

        bottomBar.add(messageInputField, BorderLayout.CENTER);
        bottomBar.add(controls, BorderLayout.EAST);
        add(bottomBar, BorderLayout.SOUTH);
    }

    private void initializePopupMenu() {
        contextPopupMenu = new JPopupMenu();

        JMenuItem deleteLastItem = new JMenuItem("Delete Last Sent Message");
        deleteLastItem.addActionListener(e -> deleteLastSentMessage());

        JMenuItem directMsgItem = new JMenuItem("Open Direct Message");
        directMsgItem.addActionListener(e -> {
            String selected = contactList.getSelectedValue();
            if (selected != null) {
                String target = extractUsername(selected);
                if (!target.equalsIgnoreCase(currentUsername)) {
                    switchToTarget(target, false);
                }
            }
        });

        JMenuItem clearScreen = new JMenuItem("Clear Chat Screen");
        clearScreen.addActionListener(e -> {
            currentChatItems.clear();
            renderHtmlChat();
        });

        contextPopupMenu.add(deleteLastItem);
        contextPopupMenu.addSeparator();
        contextPopupMenu.add(directMsgItem);
        contextPopupMenu.add(clearScreen);
    }

    private void handleSendMessage() {
        String message = messageInputField.getText().trim();
        if (message.isEmpty()) return;

        if (isTargetGroup) {
            client.sendGroupMessage(currentTarget, message);
        } else {
            client.sendDirectMessage(currentTarget, message);
        }
        messageInputField.setText("");
        messageInputField.requestFocusInWindow();
    }

    /**
     * Confirms and deletes a message (WhatsApp style)
     */
    private void confirmAndDeleteMessage(int messageId) {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Delete this message for everyone?",
                "Delete Message",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (choice == JOptionPane.YES_OPTION) {
            client.deleteMessage(messageId, currentTarget, isTargetGroup);
        }
    }

    private void deleteLastSentMessage() {
        for (int i = currentChatItems.size() - 1; i >= 0; i--) {
            ChatItem item = currentChatItems.get(i);
            if (item.isMe && item.messageId > 0) {
                confirmAndDeleteMessage(item.messageId);
                return;
            }
        }
        JOptionPane.showMessageDialog(this, "No sent message available to delete.");
    }

    /**
     * Removes deleted message from the active conversation view
     */
    public void handleMessageDeleted(int messageId, String target) {
        boolean removed = false;
        for (int i = 0; i < currentChatItems.size(); i++) {
            if (currentChatItems.get(i).messageId == messageId) {
                currentChatItems.remove(i);
                removed = true;
                break;
            }
        }
        if (removed) {
            renderHtmlChat();
            autoScroll();
        }
    }

    /**
     * Switches conversation view to a Channel or Direct User (WhatsApp style)
     */
    public void switchToTarget(String target, boolean isGroup) {
        this.currentTarget = target;
        this.isTargetGroup = isGroup;

        if (isGroup) {
            addMemberBtn.setVisible(true);
            deleteChannelBtn.setVisible(true);

            String cachedCreator = channelCreators.get(target.toLowerCase());
            if (cachedCreator != null) {
                applyGroupCreator(target, cachedCreator);
            } else {
                currentChatTitleLabel.setText("💬 #" + target + " (Channel)");
                deleteChannelBtn.setEnabled(false);
            }
            client.requestGroupCreator(target);
        } else {
            // Direct message: hide channel action buttons
            addMemberBtn.setVisible(false);
            deleteChannelBtn.setVisible(false);

            // Clear unread count for this user
            unreadCounts.remove(target.toLowerCase());
            refreshContactListDisplay();
            currentChatTitleLabel.setText("👤 Chat with @" + target);
        }

        currentChatItems.clear();
        renderHtmlChat();
        client.requestHistory(target, isGroup);
    }

    private void applyGroupCreator(String groupName, String creator) {
        if (!isTargetGroup || !currentTarget.equalsIgnoreCase(groupName)) return;

        boolean isSystem = "SYSTEM".equalsIgnoreCase(creator);
        boolean isOwner = (currentUsername != null && creator.equalsIgnoreCase(currentUsername));

        if (isSystem) {
            currentChatTitleLabel.setText("💬 #" + groupName + " (Public Channel)");
            deleteChannelBtn.setEnabled(false);
            deleteChannelBtn.setToolTipText("Default channels cannot be deleted");
        } else {
            currentChatTitleLabel.setText("💬 #" + groupName + " (Owner: @" + creator + (isOwner ? " [You]" : "") + ")");
            deleteChannelBtn.setEnabled(isOwner);
            if (isOwner) {
                deleteChannelBtn.setToolTipText("Delete channel #" + groupName + " (Owner)");
            } else {
                deleteChannelBtn.setToolTipText("Only the owner (@" + creator + ") can delete this channel");
            }
        }
    }

    public void handleGroupCreator(String groupName, String creator) {
        channelCreators.put(groupName.toLowerCase(), creator);
        applyGroupCreator(groupName, creator);
    }

    public void handleGroupDeleted(String groupName) {
        channelCreators.remove(groupName.toLowerCase());
        client.requestGroups();

        if (isTargetGroup && currentTarget.equalsIgnoreCase(groupName)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Channel #" + groupName + " was deleted by its owner.",
                    "Channel Deleted",
                    JOptionPane.INFORMATION_MESSAGE
            );
            switchToTarget("General", true);
        }
    }

    private void promptAddUserToChannel() {
        if (!isTargetGroup) return;

        String targetUser = JOptionPane.showInputDialog(
                this,
                "Enter username to add to #" + currentTarget + ":",
                "Add User to Channel",
                JOptionPane.PLAIN_MESSAGE
        );

        if (targetUser != null && !targetUser.trim().isEmpty()) {
            String trimmed = targetUser.trim();
            if (trimmed.equalsIgnoreCase(currentUsername)) {
                JOptionPane.showMessageDialog(this, "You are already in this channel!", "Add User", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            client.addGroupMember(currentTarget, trimmed);
        }
    }

    private void promptDeleteChannel() {
        if (!isTargetGroup) return;

        if (currentTarget.equalsIgnoreCase("General") || currentTarget.equalsIgnoreCase("Developers")) {
            JOptionPane.showMessageDialog(
                    this,
                    "Default channel #" + currentTarget + " cannot be deleted.",
                    "Delete Channel",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String creator = channelCreators.get(currentTarget.toLowerCase());
        if (creator != null && !creator.equalsIgnoreCase(currentUsername)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Permission Denied: Only the owner (@" + creator + ") can delete #" + currentTarget + ".",
                    "Cannot Delete Channel",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete #" + currentTarget + "?\nAll messages in this channel will be permanently removed for all members.",
                "Confirm Delete Channel",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            client.deleteGroup(currentTarget);
        }
    }

    /**
     * WhatsApp-style DM handling:
     * - If active chat is open, display immediately with status tick inside box.
     * - If user is elsewhere, show unread count on contact.
     */
    public void displayDirectMessage(String sender, String recipient, String content, String time, String status, int messageId) {
        boolean isMeSender = sender.equalsIgnoreCase(currentUsername);
        String otherParty = isMeSender ? recipient : sender;

        boolean isCurrentChatOpen = (!isTargetGroup && currentTarget.equalsIgnoreCase(otherParty));

        if (isCurrentChatOpen) {
            currentChatItems.add(new ChatItem(messageId, sender, content, time, status, isMeSender, false));
            renderHtmlChat();
            autoScroll();
        } else {
            if (!isMeSender) {
                int count = unreadCounts.getOrDefault(sender.toLowerCase(), 0) + 1;
                unreadCounts.put(sender.toLowerCase(), count);
                refreshContactListDisplay();
            }
        }
    }

    /**
     * Called when recipient reads messages or comes online:
     * Updates ticks (✓ -> ✓✓ -> Blue ✓✓) in real-time!
     */
    public void updateMessageStatus(String otherParty, String newStatus) {
        if (!isTargetGroup && currentTarget.equalsIgnoreCase(otherParty)) {
            boolean updated = false;
            for (ChatItem item : currentChatItems) {
                if (item.isMe) {
                    if ("READ".equalsIgnoreCase(newStatus)) {
                        item.status = "READ";
                        updated = true;
                    } else if ("DELIVERED".equalsIgnoreCase(newStatus) && !"READ".equalsIgnoreCase(item.status)) {
                        item.status = "DELIVERED";
                        updated = true;
                    }
                }
            }
            if (updated) {
                renderHtmlChat();
                autoScroll();
            }
        }
    }

    public void displayGroupMessage(String sender, String group, String content, String time) {
        if (isTargetGroup && currentTarget.equalsIgnoreCase(group)) {
            boolean isMe = sender.equalsIgnoreCase(currentUsername);
            currentChatItems.add(new ChatItem(0, sender, content, time, "DELIVERED", isMe, false));
            renderHtmlChat();
            autoScroll();
        }
    }

    public void displayHistory(String target, String[] entries) {
        if (!currentTarget.equalsIgnoreCase(target)) return;

        currentChatItems.clear();
        for (String entry : entries) {
            String[] parts = entry.split("::");
            if (parts.length >= 3) {
                String sender = parts[0];
                String content = parts[1];
                String time = parts[2];
                String status = (parts.length >= 4) ? parts[3] : "SENT";
                int messageId = (parts.length >= 5) ? Integer.parseInt(parts[4]) : 0;
                boolean isMe = sender.equalsIgnoreCase(currentUsername);
                currentChatItems.add(new ChatItem(messageId, sender, content, time, status, isMe, false));
            }
        }
        renderHtmlChat();
        autoScroll();
    }

    public void appendNotice(String notice) {
        currentChatItems.add(new ChatItem(0, "SYSTEM", notice, "", "", false, true));
        renderHtmlChat();
        autoScroll();
    }

    /**
     * Renders all conversation messages inside WhatsApp-style boxes/bubbles:
     * - Outgoing (You): Right-aligned light-green boxes (#D9FDD3) with time, ticks, and delete [✕] link
     * - Incoming (Contact): Left-aligned white boxes (#FFFFFF) with sender name and time
     * - System: Centered light-blue notice pill
     */
    private void renderHtmlChat() {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: sans-serif; background-color: #EFEAE2; margin: 8px;'>");
        html.append("<table width='100%' border='0' cellpadding='0' cellspacing='6'>");

        if (currentChatItems.isEmpty()) {
            html.append("<tr><td align='center'>")
                .append("<div style='color: #888888; font-style: italic; margin-top: 25px;'>")
                .append("No messages yet. Send a message to start chatting!")
                .append("</div>")
                .append("</td></tr>");
        } else {
            for (ChatItem item : currentChatItems) {
                if (item.isSystem) {
                    html.append("<tr><td align='center' style='padding: 3px 0;'>")
                        .append("<table border='0' cellpadding='4' cellspacing='0' bgcolor='#E1F3FB' style='border: 1px solid #BEE3F8;'>")
                        .append("<tr><td style='color: #4A5568; font-size: 11px; font-style: italic;'>")
                        .append("&gt;&gt; ").append(escapeHtml(item.content))
                        .append("</td></tr></table>")
                        .append("</td></tr>");
                } else if (item.isMe) {
                    // Outgoing message (You) - Right-aligned WhatsApp Green Bubble (#D9FDD3)
                    String ticksHtml = getTicksHtml(item.status);
                    String deleteLink = (item.messageId > 0) 
                            ? "<a href='delete:" + item.messageId + "' style='color: #E53935; font-size: 11px; text-decoration: none; font-weight: bold;'> ✕</a>" 
                            : "";

                    html.append("<tr><td align='right' style='padding: 2px 0;'>")
                        .append("<table border='0' cellpadding='7' cellspacing='0' bgcolor='#D9FDD3' style='border: 1px solid #B4E8AF;'>")
                        .append("<tr><td>")
                        .append("<span style='font-size: 13px; color: #111111;'>").append(escapeHtml(item.content)).append("</span><br>")
                        .append("<span style='color: #667781; font-size: 10px;'>[").append(item.time).append("]</span> ")
                        .append(ticksHtml)
                        .append(deleteLink)
                        .append("</td></tr></table>")
                        .append("</td></tr>");
                } else {
                    // Incoming message from other user - Left-aligned White Bubble (#FFFFFF)
                    html.append("<tr><td align='left' style='padding: 2px 0;'>")
                        .append("<table border='0' cellpadding='7' cellspacing='0' bgcolor='#FFFFFF' style='border: 1px solid #D5D8DC;'>")
                        .append("<tr><td>")
                        .append("<b style='color: #075E54; font-size: 11px;'>").append(escapeHtml(item.sender)).append("</b><br>")
                        .append("<span style='font-size: 13px; color: #111111;'>").append(escapeHtml(item.content)).append("</span><br>")
                        .append("<span style='color: #888888; font-size: 10px;'>[").append(item.time).append("]</span>")
                        .append("</td></tr></table>")
                        .append("</td></tr>");
                }
            }
        }

        html.append("</table></body></html>");
        chatPane.setText(html.toString());
    }

    /**
     * Generates HTML for WhatsApp-style ticks
     */
    private String getTicksHtml(String status) {
        if ("READ".equalsIgnoreCase(status)) {
            // Blue double tick (#34B7F1)
            return "<span style='color: #34B7F1; font-weight: bold; font-size: 13px;'>&#x2713;&#x2713;</span>";
        } else if ("DELIVERED".equalsIgnoreCase(status)) {
            // Grey double tick
            return "<span style='color: #888888; font-size: 13px;'>&#x2713;&#x2713;</span>";
        } else {
            // Grey single tick (SENT / recipient offline)
            return "<span style='color: #888888; font-size: 13px;'>&#x2713;</span>";
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }

    private void autoScroll() {
        if (autoScrollBox.isSelected()) {
            SwingUtilities.invokeLater(() -> {
                chatPane.setCaretPosition(chatPane.getDocument().getLength());
            });
        }
    }

    public void updateUserList(String[] userData) {
        this.lastRawUserData = (userData != null) ? userData : new String[0];
        refreshContactListDisplay();
    }

    private void refreshContactListDisplay() {
        contactListModel.clear();
        for (String data : lastRawUserData) {
            String[] parts = data.split("::");
            String username = parts[0];

            // Filter out current user - never show yourself in your contacts!
            if (currentUsername != null && !currentUsername.isEmpty() && username.equalsIgnoreCase(currentUsername)) {
                continue;
            }

            String status = (parts.length > 2) ? parts[2] : "OFFLINE";
            String indicator = status.equalsIgnoreCase("ONLINE") ? "● " : (status.equalsIgnoreCase("AWAY") ? "◐ " : "○ ");

            int unread = unreadCounts.getOrDefault(username.toLowerCase(), 0);
            String unreadBadge = (unread > 0) ? "  [" + unread + " new]" : "";

            contactListModel.addElement(indicator + username + " (" + status + ")" + unreadBadge);
        }
    }

    public void updateGroupList(String[] groups) {
        channelsGroupNode.removeAllChildren();
        for (String g : groups) {
            channelsGroupNode.add(new DefaultMutableTreeNode(g));
        }
        treeModel.reload(channelsGroupNode);
    }

    private void promptCreateChannel() {
        String name = JOptionPane.showInputDialog(this, "Enter channel name:", "New Channel", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            client.createGroup(name.trim());
        }
    }

    private String extractUsername(String contactEntry) {
        if (contactEntry == null) return "";
        String clean = contactEntry.replace("● ", "").replace("◐ ", "").replace("○ ", "");
        int idx = clean.indexOf(" (");
        if (idx != -1) {
            clean = clean.substring(0, idx);
        }
        return clean.trim();
    }

    public String getCurrentTarget() {
        return currentTarget;
    }

    public boolean isTargetGroup() {
        return isTargetGroup;
    }
}
