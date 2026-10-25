package nexuschat.ui;

import nexuschat.util.NexusConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Unit IV Syllabus:
 * - Layout Manager: GridBagLayout for clean form organization
 * - Event Handling: Delegation model, ActionEvent, KeyEvent, KeyAdapter
 * 
 * Unit V Syllabus:
 * - Swing Components: JLabel, JTextField, JPasswordField, JButton, JCheckBox
 * 
 * Simple, User-Friendly Design:
 * - Automatic default connection to local server (no confusing technical fields)
 * - Clean switch between Sign In and Create Account
 */
public class LoginPanel extends JPanel {

    public interface LoginListener {
        void onLoginSubmitted(String host, int port, String username, String password);
        void onRegisterSubmitted(String host, int port, String username, String fullName, String password);
    }

    // Connection configuration defaults
    private String serverHost = NexusConstants.DEFAULT_HOST;
    private int serverPort = NexusConstants.DEFAULT_PORT;

    private final JTextField usernameField;
    private final JTextField fullNameField;
    private final JPasswordField passwordField;
    private final JCheckBox rememberMeCheckbox;
    private final JButton primaryButton;
    private final JButton switchModeButton;
    private final JLabel fullNameLabel;
    private final JLabel titleLabel;
    private final JLabel subtitleLabel;
    private final JLabel statusLabel;
    private final JButton serverSettingsBtn;

    private boolean isRegisterMode = false;
    private LoginListener loginListener;

    public LoginPanel() {
        // Center form with clean GridBagLayout
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 1. App Title
        titleLabel = new JLabel(NexusConstants.APP_NAME, SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(new Color(30, 70, 140));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(titleLabel, gbc);

        // 2. Subtitle
        subtitleLabel = new JLabel("Welcome! Sign in to start chatting", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitleLabel.setForeground(Color.GRAY);
        gbc.gridy = 1;
        add(subtitleLabel, gbc);

        // Spacer
        gbc.gridy = 2;
        add(Box.createVerticalStrut(10), gbc);

        // 3. Username Row
        gbc.gridwidth = 1;
        gbc.gridy = 3;
        gbc.gridx = 0;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        add(userLabel, gbc);

        usernameField = new JTextField(16);
        usernameField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        gbc.gridx = 1;
        add(usernameField, gbc);

        // 4. Full Name Row (Shown only during registration)
        gbc.gridy = 4;
        gbc.gridx = 0;
        fullNameLabel = new JLabel("Your Name:");
        fullNameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        fullNameLabel.setVisible(false);
        add(fullNameLabel, gbc);

        fullNameField = new JTextField(16);
        fullNameField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        fullNameField.setVisible(false);
        gbc.gridx = 1;
        add(fullNameField, gbc);

        // 5. Password Row
        gbc.gridy = 5;
        gbc.gridx = 0;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        add(passLabel, gbc);

        passwordField = new JPasswordField(16);
        passwordField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        gbc.gridx = 1;
        add(passwordField, gbc);

        // 6. Remember Me Checkbox (Unit V)
        gbc.gridy = 6;
        gbc.gridx = 1;
        rememberMeCheckbox = new JCheckBox("Remember username", true);
        rememberMeCheckbox.setFont(new Font("SansSerif", Font.PLAIN, 11));
        add(rememberMeCheckbox, gbc);

        // 7. Primary Action Button
        gbc.gridy = 7;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        primaryButton = new JButton("Sign In");
        primaryButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        primaryButton.setPreferredSize(new Dimension(200, 34));
        add(primaryButton, gbc);

        // 8. Mode Switch Button
        gbc.gridy = 8;
        switchModeButton = new JButton("New user? Create an account");
        switchModeButton.setBorderPainted(false);
        switchModeButton.setContentAreaFilled(false);
        switchModeButton.setForeground(new Color(25, 75, 150));
        switchModeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add(switchModeButton, gbc);

        // 9. Feedback Status Label
        gbc.gridy = 9;
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        add(statusLabel, gbc);

        // 10. Simple Server Setting link at bottom
        gbc.gridy = 10;
        serverSettingsBtn = new JButton("Server: " + serverHost + ":" + serverPort + " (Change)");
        serverSettingsBtn.setBorderPainted(false);
        serverSettingsBtn.setContentAreaFilled(false);
        serverSettingsBtn.setForeground(Color.GRAY);
        serverSettingsBtn.setFont(new Font("SansSerif", Font.ITALIC, 10));
        serverSettingsBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add(serverSettingsBtn, gbc);

        // Event Listeners
        primaryButton.addActionListener(e -> handleSubmit());
        switchModeButton.addActionListener(e -> toggleMode());

        serverSettingsBtn.addActionListener(e -> promptServerSettings());

        // KeyAdapter: Pressing Enter on text fields triggers submission
        KeyAdapter enterSubmit = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleSubmit();
                }
            }
        };

        usernameField.addKeyListener(enterSubmit);
        fullNameField.addKeyListener(enterSubmit);
        passwordField.addKeyListener(enterSubmit);
    }

    public void setLoginListener(LoginListener listener) {
        this.loginListener = listener;
    }

    private void toggleMode() {
        isRegisterMode = !isRegisterMode;
        fullNameLabel.setVisible(isRegisterMode);
        fullNameField.setVisible(isRegisterMode);

        if (isRegisterMode) {
            titleLabel.setText("Create Account");
            subtitleLabel.setText("Fill in your details to register");
            primaryButton.setText("Register & Create Account");
            switchModeButton.setText("Already have an account? Sign In");
        } else {
            titleLabel.setText(NexusConstants.APP_NAME);
            subtitleLabel.setText("Welcome! Sign in to start chatting");
            primaryButton.setText("Sign In");
            switchModeButton.setText("New user? Create an account");
        }
        statusLabel.setText(" ");
        revalidate();
        repaint();
    }

    private void promptServerSettings() {
        String detectedIp = detectLocalLanIp();
        String message = "Enter Server Address (IP or Host:Port)\n"
                + "--------------------------------------------------------\n"
                + "• Connecting on this computer: 'localhost:8888'\n"
                + "• For another computer to connect to you on Wi-Fi/LAN:\n"
                + "  Have them enter your IP: '" + detectedIp + ":8888'\n"
                + "--------------------------------------------------------\n"
                + "Server Address:";

        String result = (String) JOptionPane.showInputDialog(
                this,
                message,
                "Server Connection Settings",
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                serverHost + ":" + serverPort
        );

        if (result != null && !result.trim().isEmpty()) {
            String trimmed = result.trim();
            try {
                if (trimmed.contains(":")) {
                    String[] parts = trimmed.split(":");
                    this.serverHost = parts[0].trim();
                    this.serverPort = Integer.parseInt(parts[1].trim());
                } else {
                    this.serverHost = trimmed;
                }
                serverSettingsBtn.setText("Server: " + serverHost + ":" + serverPort + " (Change)");
                setStatusSuccess("Server updated to " + serverHost + ":" + serverPort);
            } catch (Exception ex) {
                setStatusError("Invalid address. Example: " + detectedIp + ":8888");
            }
        }
    }

    private String detectLocalLanIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "192.168.1.X";
        }
    }

    private void handleSubmit() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            setStatusError("Please enter your username and password.");
            return;
        }

        if (loginListener != null) {
            if (isRegisterMode) {
                String fullName = fullNameField.getText().trim();
                if (fullName.isEmpty()) fullName = username;
                loginListener.onRegisterSubmitted(serverHost, serverPort, username, fullName, password);
            } else {
                loginListener.onLoginSubmitted(serverHost, serverPort, username, password);
            }
        }
    }

    public void setStatusError(String message) {
        statusLabel.setForeground(new Color(180, 20, 20));
        statusLabel.setText(message);
    }

    public void setStatusSuccess(String message) {
        statusLabel.setForeground(new Color(20, 130, 40));
        statusLabel.setText(message);
    }

    public void clearPassword() {
        passwordField.setText("");
    }
}
