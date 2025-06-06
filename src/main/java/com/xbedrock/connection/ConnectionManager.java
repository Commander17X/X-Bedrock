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
import java.util.concurrent.TimeUnit;

public class ConnectionManager {
    private final XBedrockPlugin plugin;
    private final Map<UUID, ConnectionInfo> connectionInfoMap;
    private final Map<String, Integer> connectionAttempts;
    private final Map<String, Long> lastConnectionTime;
    private final MiniMessage miniMessage;
    private static final int MAX_CONNECTION_ATTEMPTS = 3;
    private static final long CONNECTION_TIMEOUT = 30000; // 30 seconds
    private static final long RATE_LIMIT_WINDOW = 60000; // 1 minute
    private static final int MAX_CONNECTIONS_PER_WINDOW = 5;

    public ConnectionManager(XBedrockPlugin plugin) {
        this.plugin = plugin;
        this.connectionInfoMap = new ConcurrentHashMap<>();
        this.connectionAttempts = new HashMap<>();
        this.lastConnectionTime = new HashMap<>();
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void handleConnectionRequest(ConnectionRequestEvent event) {
        String address = event.getConnection().getSocketAddress().getAddress().getHostAddress();

        // Check rate limiting
        if (isRateLimited(address)) {
            event.setCancelled(true);
            event.getConnection().disconnect(plugin.getMessageManager().parseMessage("rate_limited"));
            return;
        }

        // Check connection attempts
        int attempts = connectionAttempts.getOrDefault(address, 0);
        if (attempts >= MAX_CONNECTION_ATTEMPTS) {
            event.setCancelled(true);
            event.getConnection().disconnect(plugin.getMessageManager().parseMessage("too_many_attempts"));
            return;
        }

        // Update connection attempts
        connectionAttempts.put(address, attempts + 1);
        lastConnectionTime.put(address, System.currentTimeMillis());

        // Create connection info
        ConnectionInfo info = new ConnectionInfo(event.getConnection());
        connectionInfoMap.put(event.getConnection().getAuthData().getUuid(), info);

        // Start connection timeout
        startConnectionTimeout(event.getConnection());
    }

    public void handleConnectionSuccess(ConnectionSuccessEvent event) {
        UUID uuid = event.getConnection().getAuthData().getUuid();
        ConnectionInfo info = connectionInfoMap.get(uuid);

        if (info != null) {
            info.setConnected(true);
            info.setConnectionTime(System.currentTimeMillis());

            // Reset connection attempts
            String address = event.getConnection().getSocketAddress().getAddress().getHostAddress();
            connectionAttempts.remove(address);

            // Send welcome message
            event.getConnection().sendMessage(plugin.getMessageManager().parseMessage("welcome"));

            // Initialize player data
            plugin.getPlayerDataManager().initializePlayerData(event.getConnection());
        }
    }

    public void handleConnectionClose(ConnectionCloseEvent event) {
        UUID uuid = event.getConnection().getAuthData().getUuid();
        ConnectionInfo info = connectionInfoMap.remove(uuid);

        if (info != null) {
            // Save player data
            plugin.getPlayerDataManager().savePlayerData(event.getConnection());

            // Clean up resources
            info.cleanup();
        }
    }

    private boolean isRateLimited(String address) {
        long currentTime = System.currentTimeMillis();
        long lastTime = lastConnectionTime.getOrDefault(address, 0L);

        if (currentTime - lastTime > RATE_LIMIT_WINDOW) {
            lastConnectionTime.remove(address);
            return false;
        }

        return connectionAttempts.getOrDefault(address, 0) >= MAX_CONNECTIONS_PER_WINDOW;
    }

    private void startConnectionTimeout(GeyserConnection connection) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ConnectionInfo info = connectionInfoMap.get(connection.getAuthData().getUuid());
            if (info != null && !info.isConnected()) {
                connection.disconnect(plugin.getMessageManager().parseMessage("connection_timeout"));
                connectionInfoMap.remove(connection.getAuthData().getUuid());
            }
        }, TimeUnit.MILLISECONDS.toSeconds(CONNECTION_TIMEOUT) * 20L);
    }

    public static class ConnectionInfo {
        private final GeyserConnection connection;
        private boolean connected;
        private long connectionTime;
        private String clientVersion;
        private String deviceModel;
        private String deviceOS;

        public ConnectionInfo(GeyserConnection connection) {
            this.connection = connection;
            this.connected = false;
            this.connectionTime = 0;
            this.clientVersion = connection.getClientVersion();
            this.deviceModel = connection.getDeviceModel();
            this.deviceOS = connection.getDeviceOS();
        }

        public void cleanup() {
            // Clean up any resources
        }

        // Getters and setters
        public boolean isConnected() {
            return connected;
        }

        public void setConnected(boolean connected) {
            this.connected = connected;
        }

        public long getConnectionTime() {
            return connectionTime;
        }

        public void setConnectionTime(long connectionTime) {
            this.connectionTime = connectionTime;
        }

        public String getClientVersion() {
            return clientVersion;
        }

        public String getDeviceModel() {
            return deviceModel;
        }

        public String getDeviceOS() {
            return deviceOS;
        }
    }
}