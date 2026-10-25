package nexuschat.network;

import nexuschat.util.NexusConstants;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

/**
 * Unit III Syllabus: Multithreading & Background Thread Management
 * 
 * Automatically ensures that the local NexusChat server is running
 * whenever a user starts any client or launcher.
 * 
 * - If a server is already listening on port 8888, it does nothing and reuses it.
 * - If no server is running, it automatically spawns NexusServer as an independent
 *   background process so that closing any client window NEVER terminates the
 *   server for other active clients or remote peers.
 * - Provides fallback to an in-process thread if detached process spawning is restricted.
 */
public final class ServerBootstrap {

    private static NexusServer embeddedServer = null;

    private ServerBootstrap() {}

    /**
     * Checks if the server is running on localhost:port.
     * If not, starts it automatically as an independent background process.
     * 
     * @param port The server port (default 8888)
     * @return true if the server is active and accepting connections; false otherwise
     */
    public static synchronized boolean ensureServerRunning(int port) {
        // Step 1: Probe if server is already active
        if (isPortListening("localhost", port)) {
            return true;
        }

        // Step 2: Not running - spawn independent background server process
        try {
            String javaHome = System.getProperty("java.home");
            String javaw = javaHome + File.separator + "bin" + File.separator + "javaw";
            if (!new File(javaw + ".exe").exists() && !new File(javaw).exists()) {
                javaw = javaHome + File.separator + "bin" + File.separator + "java";
            }

            File codeSource = null;
            try {
                codeSource = new File(ServerBootstrap.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            } catch (Exception ignored) {}

            ProcessBuilder pb;
            if (codeSource != null && codeSource.isFile() && codeSource.getName().toLowerCase().endsWith(".jar")) {
                // When launched from standalone executable JAR
                pb = new ProcessBuilder(
                        javaw,
                        "-jar",
                        codeSource.getAbsolutePath(),
                        "--server",
                        String.valueOf(port)
                );
            } else {
                // When launched from IDE or class files
                String classPath = System.getProperty("java.class.path");
                pb = new ProcessBuilder(
                        javaw,
                        "-cp",
                        classPath,
                        "nexuschat.network.NexusServer",
                        String.valueOf(port)
                );
            }

            File workingDir = (codeSource != null && codeSource.isFile()) ? codeSource.getParentFile() : new File(".");
            pb.directory(workingDir);

            // Detach child standard I/O so child process is fully detached from parent
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);

            pb.start();

            // Step 3: Wait up to 4 seconds for ServerSocket to bind and start accepting
            for (int i = 0; i < 40; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}

                if (isPortListening("localhost", port)) {
                    System.out.println("[ServerBootstrap] Independent background NexusChat server active on port " + port);
                    return true;
                }
            }
        } catch (Exception ex) {
            System.err.println("[ServerBootstrap] ProcessBuilder spawn error: " + ex.getMessage());
        }

        // Step 4: Fallback to in-process daemon thread if process spawning failed
        if (!isPortListening("localhost", port)) {
            try {
                embeddedServer = new NexusServer(port);
                Thread serverThread = new Thread(() -> embeddedServer.start(), "NexusChat-FallbackServer-Thread");
                serverThread.setDaemon(true);
                serverThread.start();

                for (int i = 0; i < 30; i++) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {}

                    if (isPortListening("localhost", port)) {
                        return true;
                    }
                }
            } catch (Exception ex) {
                System.err.println("[ServerBootstrap] Fallback server error: " + ex.getMessage());
            }
        }

        return isPortListening("localhost", port);
    }

    /**
     * Checks if a socket connection can be established.
     */
    public static boolean isPortListening(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    /**
     * Gracefully stops the embedded server if one was started.
     */
    public static synchronized void stopEmbeddedServer() {
        if (embeddedServer != null) {
            try {
                embeddedServer.stop();
            } catch (Exception ignored) {}
            embeddedServer = null;
        }
    }
}
