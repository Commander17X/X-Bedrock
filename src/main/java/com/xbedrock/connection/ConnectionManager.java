package com.xbedrock.connection;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.connection.ConnectionRequestEvent;
import org.geysermc.geyser.api.event.connection.ConnectionSuccessEvent;
import org.geysermc.geyser.api.event.connection.ConnectionCloseEvent;
import com.xbedrock.XBedrockPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    private final XBedrockPlugin plugin;
    private final Map<UUID, ConnectionInfo> connectionInfoMap;
    private final Map<String, Integer> connectionAttempts;
    private final MiniMessage miniMessage;
    private static final int MAX_CONNECTION_ATTEMPTS = 3;
    private static final long CONNECTION_TIMEOUT = 30000; // 30 seconds

    public ConnectionManager(XBedrockPlugin plugin) {
        this.plugin = plugin;
        this.connectionInfoMap = new ConcurrentHashMap<>();
        this.connectionAttempts = new HashMap<>();
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void handleConnectionRequest(ConnectionRequestEvent event) {
        GeyserConnection connection = event.connection();
        String address = connection.remoteAddress();
        InetAddress ipAddress = connection.remoteAddress().getAddress();

        // Check for connection attempts
        int attempts = connectionAttempts.getOrDefault(address, 0);
        if (attempts >= MAX_CONNECTION_ATTEMPTS) {
            event.setCancelled(true);
            sendMessage(connection, "<red>Too many connection attempts. Please try again later.");
            return;
        }

        // Validate IP address
        if (!isValidIPAddress(ipAddress)) {
            event.setCancelled(true);
            sendMessage(connection, "<red>Invalid IP address! Please check your connection settings.");
            return;
        }

        // Store connection info
        ConnectionInfo info = new ConnectionInfo(System.currentTimeMillis());
        connectionInfoMap.put(connection.bedrockUuid(), info);
        connectionAttempts.put(address, attempts + 1);

        // Send welcome message
        sendMessage(connection, "<gradient:#00ff00:#0000ff>Welcome to the server!</gradient>");
        sendMessage(connection, "<gray>Connecting to Java server...</gray>");
    }

    public void handleConnectionSuccess(ConnectionSuccessEvent event) {
        GeyserConnection connection = event.connection();
        String address = connection.remoteAddress();

        // Clear connection attempts
        connectionAttempts.remove(address);

        // Send success message
        sendMessage(connection, "<green>Successfully connected to the server!");
        sendMessage(connection, "<yellow>Type /help for available commands");

        // Handle command menu freezing
        scheduleCommandMenuLoad(connection);
    }

    public void handleConnectionClose(ConnectionCloseEvent event) {
        GeyserConnection connection = event.connection();
        connectionInfoMap.remove(connection.bedrockUuid());
        connectionAttempts.remove(connection.remoteAddress());
    }

    private void scheduleCommandMenuLoad(GeyserConnection connection) {
        // Schedule command menu load after a short delay to prevent freezing
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (connection.isConnected()) {
                // Pre-load command menu
                connection.sendCommand("help");
            }
        }, 20L); // 1 second delay
    }

    private boolean isValidIPAddress(InetAddress address) {
        if (address == null)
            return false;

        // Check if it's a valid IPv4 or IPv6 address
        String hostAddress = address.getHostAddress();
        return hostAddress.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$") || // IPv4
                hostAddress.matches("^(?:[A-F0-9]{1,4}:){7}[A-F0-9]{1,4}$"); // IPv6
    }

    public void sendMessage(GeyserConnection connection, String message) {
        Component component = miniMessage.deserialize(message);
        connection.sendMessage(component);
    }

    public void broadcastMessage(String message) {
        Component component = miniMessage.deserialize(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(component);
        }
    }

    private static class ConnectionInfo {
        private final long connectionTime;

        public ConnectionInfo(long connectionTime) {
            this.connectionTime = connectionTime;
        }

        public boolean isTimedOut() {
            return System.currentTimeMillis() - connectionTime > CONNECTION_TIMEOUT;
        }
    }
}