# NexusChat: Multi-User Messaging Application

[![Java](https://img.shields.io/badge/Java-JDK%2017%2B%20%7C%2021%2B%20%7C%2025-orange.svg)](https://www.oracle.com/java/)
[![GUI](https://img.shields.io/badge/GUI-Swing-blue.svg)]()
[![Database](https://img.shields.io/badge/Database-SQLite%20JDBC-green.svg)](https://github.com/xerial/sqlite-jdbc)
[![Architecture](https://img.shields.io/badge/Architecture-Client--Server%20Sockets-purple.svg)]()
[![Security](https://img.shields.io/badge/Security-AES%20%7C%20SHA--256-blueviolet.svg)]()

---

## Table of Contents
- [Overview](#overview)
- [Key Features](#key-features)
- [Downloads and Releases](#downloads-and-releases)
- [Running on a Single Computer](#running-on-a-single-computer)
- [Running Across Multiple Computers (LAN / Wi-Fi)](#running-across-multiple-computers-lan--wi-fi)
  - [Scenario A: Same Local Network / Wi-Fi / Hotspot](#scenario-a-same-local-network--wi-fi--hotspot)
  - [Scenario B: Remote Connections (Tailscale / ngrok)](#scenario-b-remote-connections-tailscale--ngrok)
- [IntelliJ IDEA Setup](#intellij-idea-setup)
- [Project Directory Structure](#project-directory-structure)
- [Troubleshooting](#troubleshooting)

---

## Overview

NexusChat is a client-server desktop messaging application written in Java. It provides private one-on-one direct messaging (with delivery and read status ticks, chat bubbles, unread counts, and real-time message deletion) and group channels with owner permissions. 

The application is packaged as standalone executable JAR files with embedded SQLite JDBC and SLF4J dependencies. It runs directly via double-click or command line on Windows, macOS, and Linux without any batch scripts or external database setup.

---

## Key Features

### Direct Messaging
- **Message Bubbles**: Outgoing messages appear on the right in light-green boxes (`#D9FDD3`); incoming messages appear on the left in white boxes (`#FFFFFF`).
- **Status Ticks**:
  - Grey single tick (`✓`): Message sent to server (recipient offline).
  - Grey double tick (`✓✓`): Message delivered to recipient client.
  - Blue double tick (`✓✓`): Recipient opened and viewed the conversation.
- **Message Deletion**: Users can delete sent messages by clicking `[✕]` inside the bubble or via right-click context menu. Messages are removed in real time from the database and both participants' screens.
- **Isolated Chats**: Selecting a contact opens that specific user's conversation history. Incoming messages from other contacts update an unread counter (e.g. `[2 new]`) without interrupting the active chat.
- **Self Filtering**: The logged-in user is excluded from their own contacts list.

### Channels and Group Chat
- **Public Channels**: Default channels (`#General`, `#Developers`) are open to all users.
- **Custom Channels**: Users can create custom channels. The creator is stored as the owner (`created_by`).
- **Owner-Only Deletion**: Only the creator who created a channel can delete it. Default channels cannot be deleted.
- **Add Users by Username**: Channel members can add other users directly by entering their username.

### Background Server Management
- **Process Decoupling**: When starting `NexusChat.jar`, the server is spawned as an independent background process (`ProcessBuilder`). Closing a client window does not kill the server for other active clients.
- **Automatic Shutdown**: When the last remaining client window is closed, the server detects zero active connections, waits a 5-second grace period, and shuts itself down cleanly.

### Security and Storage Encryption
- **Password Protection**: Account passwords are stored as salted SHA-256 hashes (`SecurityUtil`) rather than plaintext.
- **Message Encryption**: Message payloads are encrypted with AES (`AesEncryptionService`) before being stored in the database.

### Utilities
- **User Directory**: View registered users in a Swing `JTable`.
- **Recursive Search**: Search message histories recursively (`HistorySearchUtil`).
- **Transcript Export**: Export conversations to local text files using Java `BufferedWriter`.

---

## Downloads and Releases

Pre-compiled standalone JAR files are available on the [Releases](../../releases) tab:

| File | Description | Execution |
| :--- | :--- | :--- |
| `NexusChat.jar` | Universal executable. Automatically starts local server if not running and opens client GUI. | Double-click or `java -jar NexusChat.jar` |
| `NexusChat-Client.jar` | Standalone client GUI. Connects to an existing server. | Double-click or `java -jar NexusChat-Client.jar` |
| `NexusChat-Server.jar` | Standalone server. Headless console mode. | `java -jar NexusChat-Server.jar` |

---

## Running on a Single Computer

### Option 1: Double-Click
1. Double-click `NexusChat.jar`.
2. To test two users chatting locally:
   - In the menu bar of the first window, select **Chat -> Open New Chat Window...** (or double-click `NexusChat.jar` a second time).
3. Register two accounts (e.g., `alice` and `bob`) and begin messaging.

### Option 2: Command Line
```bash
# Launch universal client (auto-starts local server on port 8888 if needed)
java -jar NexusChat.jar

# Or run server explicitly on custom port
java -jar NexusChat.jar --server 8888

# Or launch client only
java -jar NexusChat-Client.jar
```

---

## Running Across Multiple Computers (LAN / Wi-Fi)

NexusChat communicates over standard TCP/IP sockets (`java.net.ServerSocket` and `java.net.Socket`).

```
[Computer A - Host (e.g. 192.168.1.15)]
      NexusServer on port 8888 <---> SQLite Database
      NexusChat Client (User 1)
               ^
               | TCP Connection
               v
[Computer B - Client (User 2)]
      NexusChat Client connects to 192.168.1.15:8888
```

### Scenario A: Same Local Network / Wi-Fi / Hotspot

#### Step 1: On Host Computer (Computer A)
1. Run `NexusChat.jar`. The server binds to port 8888 on all network interfaces.
2. On the login screen, click the link at the bottom: `Server: localhost:8888 (Change)`.
3. Note down the detected local IP address shown (e.g., `192.168.1.15` or `10.x.x.x`).
4. If Windows Firewall prompts to allow network access, select Allow Access for Private networks.
5. Sign in to your account.

#### Step 2: On Second Computer (Computer B)
1. Copy `NexusChat.jar` to Computer B.
2. Run `NexusChat.jar`.
3. On the login screen, click `Server: localhost:8888 (Change)`.
4. Enter Computer A's IP address (e.g., `192.168.1.15:8888` or `192.168.1.15`) and click OK.
5. Click "New user? Create an account", register a username, and log in.

Both computers are now connected to the same chat server.

---

### Scenario B: Remote Connections (Tailscale / ngrok)

If computers are on different networks:

#### Option 1: Tailscale / ZeroTier (Mesh VPN)
1. Install Tailscale on both machines and join the same account.
2. Enter Host's Tailscale IP (e.g., `100.x.y.z:8888`) into the second computer's server settings.

#### Option 2: ngrok (TCP Tunnel)
1. On Host: run `ngrok tcp 8888`.
2. On Second Computer: enter the forwarding address (e.g., `4.tcp.ngrok.io:12345`) into server settings.

---

## IntelliJ IDEA Setup

The repository includes pre-configured IntelliJ IDEA project files (`.idea/` and `NexusChat.iml`):

1. Open IntelliJ IDEA.
2. Select **File -> Open...** and choose the repository root directory.
3. Pre-configured run configurations are available in the top-right toolbar:
   - `NexusChat Launcher`: Launches main application.
   - `NexusChat Client (GUI)`: Launches GUI client directly.
   - `NexusChat Server`: Runs headless server.
4. To rebuild artifacts in IntelliJ: go to **Build -> Build Artifacts... -> NexusChat:jar -> Build**.

---

## Project Directory Structure

```text
NexusChat/
├── NexusChat.jar                     # Standalone Universal Executable JAR
├── NexusChat-Client.jar              # Client Executable JAR
├── NexusChat-Server.jar              # Server Executable JAR
├── NexusChat.iml                     # IntelliJ Module File
├── pom.xml                           # Maven Build Configuration
├── .idea/                            # IntelliJ Run Configurations & Settings
├── lib/                              # External Libraries
│   ├── sqlite-jdbc-3.45.1.0.jar      # SQLite JDBC Driver
│   ├── slf4j-api-2.0.12.jar          # Logging API
│   └── slf4j-simple-2.0.12.jar       # Logger Implementation
├── src/nexuschat/
│   ├── NexusChatLauncher.java        # Main launcher and bootstrap
│   ├── model/                        # Domain entities and models
│   ├── dao/                          # Database operations and JDBC manager
│   ├── exception/                    # Custom exception classes
│   ├── network/                      # Server, Client, Protocol, Handlers
│   ├── service/                      # Chat business logic, export, AES encryption
│   ├── util/                         # Utilities, search, constants, password hashing
│   └── ui/                           # Swing panels, dialogs, and main frame
├── .gitignore                        # Git exclusion rules (ignores *.db and caches)
├── README.md                         # Application documentation
└── PROJECT_REPORT.md                 # Architecture and technical report
```

---

## Troubleshooting

### "Unable to connect to NexusChat Server at localhost:8888"
- `NexusChat.jar` automatically starts the local background server if connecting to `localhost`.
- If connecting to another computer, verify that both computers are on the same Wi-Fi network and that the host IP was entered correctly in the server settings link.
- Ensure host firewall allows Java on Private networks.

### "Port 8888 is already in use"
- Start the server on a different port:
  ```bash
  java -jar NexusChat.jar --server 9000
  ```
- In the client, update server settings to connect to port `9000`.

### Message Deletion
- Click `[✕]` inside a sent bubble or right-click to delete. Deleted messages are removed from the database and updated in real time for all clients.

### Channel Permissions
- Only the user who created a channel can delete it. Default channels (`#General`, `#Developers`) cannot be deleted.

### Storage and Security
- `nexuschat.db` is created and managed automatically in the working directory on first launch.
- Messages are stored using 128-bit AES encryption at rest, and account passwords are saved as salted SHA-256 hashes without requiring manual configuration.
