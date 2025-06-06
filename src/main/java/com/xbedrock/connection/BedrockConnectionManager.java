package com.xbedrock.connection;

import com.xbedrock.XBedrockPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class BedrockConnectionManager implements Listener {
    private final XBedrockPlugin plugin;
    private final Map<UUID, BedrockConnection> connections;
    private final Map<String, String> deviceInfo;
    private final Map<UUID, Long> lastPositionUpdate;
    private BukkitTask pingTask;
    private static final long POSITION_UPDATE_INTERVAL = 50; // 2.5 seconds
    private static final int PING_UPDATE_INTERVAL = 100; // 5 seconds

    public BedrockConnectionManager(XBedrockPlugin plugin) {
        this.plugin = plugin;
        this.connections = new ConcurrentHashMap<>();
        this.deviceInfo = new ConcurrentHashMap<>();
        this.lastPositionUpdate = new ConcurrentHashMap<>();

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Start ping task
        startPingTask();
    }

    private void startPingTask() {
        pingTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, BedrockConnection> entry : connections.entrySet()) {
                Player player = plugin.getServer().getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    try {
                        int ping = getPlayerPing(player);
                        entry.getValue().setPing(ping);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Failed to update ping for " + player.getName(), e);
                    }
                }
            }
        }, PING_UPDATE_INTERVAL, PING_UPDATE_INTERVAL);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!isBedrockPlayer(event.getPlayer()))
            return;

        try {
            String address = event.getAddress().getHostAddress();
            String deviceId = extractDeviceId(event.getPlayer());
            String deviceModel = extractDeviceModel(event.getPlayer());
            String deviceOS = extractDeviceOS(event.getPlayer());
            String clientVersion = extractClientVersion(event.getPlayer());
            String language = extractLanguage(event.getPlayer());
            boolean isPremium = isPremiumPlayer(event.getPlayer());

            BedrockConnection connection = new BedrockConnection(
                    event.getPlayer().getUniqueId(),
                    event.getPlayer().getName(),
                    deviceId,
                    deviceModel,
                    deviceOS,
                    clientVersion,
                    language,
                    isPremium);

            connections.put(event.getPlayer().getUniqueId(), connection);
            deviceInfo.put(deviceId, address);
            lastPositionUpdate.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());

            // Initialize player data
            plugin.getPlayerDataManager().initializePlayerData(connection);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to initialize Bedrock connection for " + event.getPlayer().getName(), e);
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "§cFailed to initialize Bedrock connection");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        BedrockConnection connection = connections.get(event.getPlayer().getUniqueId());
        if (connection != null) {
            connection.setConnected(true);
            updatePlayerPosition(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        BedrockConnection connection = connections.remove(event.getPlayer().getUniqueId());
        if (connection != null) {
            connection.setConnected(false);
            deviceInfo.remove(connection.getDeviceId());
            lastPositionUpdate.remove(event.getPlayer().getUniqueId());

            // Save player data
            plugin.getPlayerDataManager().savePlayerData(connection);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedPosition())
            return;

        UUID uuid = event.getPlayer().getUniqueId();
        if (!connections.containsKey(uuid))
            return;

        long currentTime = System.currentTimeMillis();
        Long lastUpdate = lastPositionUpdate.get(uuid);

        if (lastUpdate == null || currentTime - lastUpdate >= POSITION_UPDATE_INTERVAL) {
            updatePlayerPosition(event.getPlayer());
            lastPositionUpdate.put(uuid, currentTime);
        }
    }

    private void updatePlayerPosition(Player player) {
        BedrockConnection connection = connections.get(player.getUniqueId());
        if (connection != null) {
            connection.updatePosition(
                    player.getWorld().getName(),
                    player.getLocation().getX(),
                    player.getLocation().getY(),
                    player.getLocation().getZ(),
                    player.getLocation().getYaw(),
                    player.getLocation().getPitch());
        }
    }

    private boolean isBedrockPlayer(Player player) {
        try {
            Object connection = player.getClass().getMethod("getHandle").invoke(player);
            return connection.getClass().getSimpleName().contains("Bedrock");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to check if player is Bedrock", e);
            return false;
        }
    }

    private String extractDeviceId(Player player) {
        return extractDeviceInfo(player, "getDeviceId", "unknown");
    }

    private String extractDeviceModel(Player player) {
        return extractDeviceInfo(player, "getDeviceModel", "unknown");
    }

    private String extractDeviceOS(Player player) {
        return extractDeviceInfo(player, "getDeviceOS", "unknown");
    }

    private String extractClientVersion(Player player) {
        return extractDeviceInfo(player, "getClientVersion", "unknown");
    }

    private String extractLanguage(Player player) {
        return extractDeviceInfo(player, "getLanguage", "en_US");
    }

    private boolean isPremiumPlayer(Player player) {
        try {
            Object connection = player.getClass().getMethod("getHandle").invoke(player);
            Object deviceInfo = connection.getClass().getMethod("getDeviceInfo").invoke(connection);
            Method method = deviceInfo.getClass().getMethod("isPremium");
            return (boolean) method.invoke(deviceInfo);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to check premium status", e);
            return false;
        }
    }

    private String extractDeviceInfo(Player player, String methodName, String defaultValue) {
        try {
            Object connection = player.getClass().getMethod("getHandle").invoke(player);
            Object deviceInfo = connection.getClass().getMethod("getDeviceInfo").invoke(connection);
            Method method = deviceInfo.getClass().getMethod(methodName);
            return (String) method.invoke(deviceInfo);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to extract " + methodName, e);
            return defaultValue;
        }
    }

    private int getPlayerPing(Player player) {
        try {
            Object connection = player.getClass().getMethod("getHandle").invoke(player);
            return (int) connection.getClass().getMethod("getPing").invoke(connection);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get player ping", e);
            return -1;
        }
    }

    public BedrockConnection getConnection(UUID uuid) {
        return connections.get(uuid);
    }

    public boolean isBedrockConnection(UUID uuid) {
        return connections.containsKey(uuid);
    }

    public void shutdown() {
        if (pingTask != null) {
            pingTask.cancel();
        }
        connections.clear();
        deviceInfo.clear();
        lastPositionUpdate.clear();
    }
}