package nexuschat;

import nexuschat.network.NexusServer;
import nexuschat.network.ServerBootstrap;
import nexuschat.ui.NexusChatApp;
import nexuschat.util.NexusConstants;

import javax.swing.*;
import java.io.IOException;
import java.net.Socket;

/**
 * Main Executable Entry Point for NexusChat.jar
 * 
 * User-Friendly Launcher:
 * - When double-clicked or run:
 *   1. Automatically checks if the local NexusChat server is running.
 *   2. If not running, launches the server automatically in the background.
 *   3. Opens the clean, friendly NexusChat messaging application window.
 * - Supports CLI flag '--server' for dedicated background server mode.
 */
public class NexusChatLauncher {

    public static void main(String[] args) {
        // Dedicated server mode flag
        if (args.length > 0) {
            String arg = args[0].trim().toLowerCase();
            if (arg.equals("--server") || arg.equals("-s") || arg.equals("server")) {
                int port = NexusConstants.DEFAULT_PORT;
                if (args.length > 1) {
                    try {
                        port = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {}
                }
                System.out.println("Starting NexusChat Server on port " + port + "...");
                NexusServer server = new NexusServer(port);
                server.setAutoCloseWhenEmpty(true);
                server.setExitOnShutdown(true);
                server.start();
                return;
            }
        }

        // Automatic user-friendly startup:
        // Ensure the server is active on localhost:8888
        ServerBootstrap.ensureServerRunning(NexusConstants.DEFAULT_PORT);

        // Open the clean, simple NexusChat Client window
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            NexusChatApp app = new NexusChatApp();
            app.setVisible(true);
        });
    }
}
