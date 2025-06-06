package com.xbedrock.connection;

import com.xbedrock.XBedrockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class ConnectionGate implements Listener {
    private final XBedrockPlugin plugin;
    private final Map<UUID, ConnectionInfo> connectionQueue;
    private final Map<String, AtomicInteger> connectionAttempts;
    private final Map<UUID, Long> lastConnectionTime;
    private final AtomicInteger currentConnections;
    private BukkitTask queueProcessor;

    // Configuration
    private int maxConnectionsPerSecond;
    private int maxQueueSize;
    private int maxConnectionsPerIP;
    private int queueProcessInterval;
    private int peakHourMultiplier;
    private List<Integer> peakHours;

    public ConnectionGate(XBedrockPlugin plugin) {
        this.plugin = plugin;
        this.connectionQueue = new ConcurrentHashMap<>();
        this.connectionAttempts = new ConcurrentHashMap<>();
        this.lastConnectionTime = new ConcurrentHashMap<>();
        this.currentConnections = new AtomicInteger(0);

        // Load configuration
        loadConfig();

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Start queue processor
        startQueueProcessor();
    }

    private void loadConfig() {
        // Default values
        this.maxConnectionsPerSecond = 20;
        this.maxQueueSize = 100;
        this.maxConnectionsPerIP = 3;
        this.queueProcessInterval = 20; // 1 second
        this.peakHourMultiplier = 2;
        this.peakHours = Arrays.asList(16, 17, 18, 19, 20, 21, 22); // 4 PM - 10 PM

        // Load from config if available
        if (plugin.getConfig().contains("connection-gate")) {
            this.maxConnectionsPerSecond = plugin.getConfig().getInt("connection-gate.max-connections-per-second", 20);
            this.maxQueueSize = plugin.getConfig().getInt("connection-gate.max-queue-size", 100);
            this.maxConnectionsPerIP = plugin.getConfig().getInt("connection-gate.max-connections-per-ip", 3);
            this.queueProcessInterval = plugin.getConfig().getInt("connection-gate.queue-process-interval", 20);
            this.peakHourMultiplier = plugin.getConfig().getInt("connection-gate.peak-hour-multiplier", 2);
            this.peakHours = plugin.getConfig().getIntegerList("connection-gate.peak-hours");
        }
    }

    private void startQueueProcessor() {
        queueProcessor = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (connectionQueue.isEmpty())
                return;

            int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            int maxConnections = isPeakHour(currentHour) ? maxConnectionsPerSecond * peakHourMultiplier
                    : maxConnectionsPerSecond;

            int processed = 0;
            Iterator<Map.Entry<UUID, ConnectionInfo>> iterator = connectionQueue.entrySet().iterator();

            while (iterator.hasNext() && processed < maxConnections) {
                Map.Entry<UUID, ConnectionInfo> entry = iterator.next();
                ConnectionInfo info = entry.getValue();

                if (System.currentTimeMillis() - info.getQueueTime() > 30000) { // 30 second timeout
                    iterator.remove();
                    continue;
                }

                if (canConnect(info)) {
                    processConnection(info);
                    iterator.remove();
                    processed++;
                }
            }
        }, 0L, queueProcessInterval);
    }

    private boolean isPeakHour(int hour) {
        return peakHours.contains(hour);
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!isBedrockPlayer(event.getPlayer()))
            return;

        String address = event.getAddress().getHostAddress();
        ConnectionInfo info = new ConnectionInfo(
                event.getPlayer().getUniqueId(),
                event.getPlayer().getName(),
                address,
                System.currentTimeMillis());

        if (!canConnect(info)) {
            if (connectionQueue.size() >= maxQueueSize) {
                event.disallow(PlayerLoginEvent.Result.KICK_FULL,
                        "§cServer is full. Please try again later.");
                return;
            }

            connectionQueue.put(event.getPlayer().getUniqueId(), info);
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    "§aYou have been placed in queue. Position: " +
                            connectionQueue.size() + "/" + maxQueueSize);
        }
    }

    private boolean canConnect(ConnectionInfo info) {
        // Check connection attempts
        AtomicInteger attempts = connectionAttempts.computeIfAbsent(
                info.getAddress(), k -> new AtomicInteger(0));

        if (attempts.get() >= maxConnectionsPerIP) {
            return false;
        }

        // Check last connection time
        Long lastTime = lastConnectionTime.get(info.getUuid());
        if (lastTime != null && System.currentTimeMillis() - lastTime < 1000) {
            return false;
        }

        return true;
    }

    private void processConnection(ConnectionInfo info) {
        currentConnections.incrementAndGet();
        connectionAttempts.get(info.getAddress()).incrementAndGet();
        lastConnectionTime.put(info.getUuid(), System.currentTimeMillis());

        // Schedule cleanup
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            currentConnections.decrementAndGet();
            connectionAttempts.get(info.getAddress()).decrementAndGet();
        }, 6000L); // 5 minutes
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

    public void shutdown() {
        if (queueProcessor != null) {
            queueProcessor.cancel();
        }
    }

    public int getQueueSize() {
        return connectionQueue.size();
    }

    public int getCurrentConnections() {
        return currentConnections.get();
    }

    public void updateConfig() {
        loadConfig();
    }
}