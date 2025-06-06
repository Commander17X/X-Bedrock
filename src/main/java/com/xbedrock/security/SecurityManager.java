package com.xbedrock.security;

import com.xbedrock.XBedrockPlugin;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class SecurityManager implements Listener {
    private final XBedrockPlugin plugin;
    private final Map<UUID, PlayerSecurityData> playerData;
    private final Map<String, AtomicInteger> packetViolations;
    private final Map<UUID, Long> lastPacketTime;
    private final Map<UUID, Integer> printerModePlayers;

    // Configuration values
    private static final int MAX_PACKETS_PER_SECOND = 100;
    private static final int MAX_PACKET_SIZE = 2097152; // 2MB
    private static final int VIOLATION_THRESHOLD = 5;
    private static final long PACKET_TIMEOUT = 1000; // 1 second
    private static final int PRINTER_MODE_PACKET_THRESHOLD = 50;

    public SecurityManager(XBedrockPlugin plugin) {
        this.plugin = plugin;
        this.playerData = new ConcurrentHashMap<>();
        this.packetViolations = new ConcurrentHashMap<>();
        this.lastPacketTime = new ConcurrentHashMap<>();
        this.printerModePlayers = new ConcurrentHashMap<>();

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Initialize Netty handlers
        initializeNettyHandlers();
    }

    private void initializeNettyHandlers() {
        // Add Netty handler to all channels
        plugin.getServer().getOnlinePlayers().forEach(this::addNettyHandler);
    }

    private void addNettyHandler(Player player) {
        try {
            Object connection = player.getClass().getMethod("getHandle").invoke(player);
            Object networkManager = connection.getClass().getField("b").get(connection);
            Channel channel = (Channel) networkManager.getClass().getMethod("getChannel").invoke(networkManager);

            // Add our custom handler
            channel.pipeline().addBefore("packet_handler", "xbedrock_security", new ChannelInboundHandlerAdapter() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    if (!handlePacket(player, msg)) {
                        // Drop the packet if it's malicious
                        return;
                    }
                    super.channelRead(ctx, msg);
                }
            });
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to add Netty handler for " + player.getName(), e);
        }
    }

    private boolean handlePacket(Player player, Object packet) {
        UUID playerId = player.getUniqueId();
        String packetName = packet.getClass().getSimpleName();
        long currentTime = System.currentTimeMillis();

        // Initialize player data if needed
        playerData.computeIfAbsent(playerId, k -> new PlayerSecurityData());
        PlayerSecurityData data = playerData.get(playerId);

        // Check for Netty crasher
        if (isNettyCrasher(packet)) {
            handleViolation(player, "NettyCrasher");
            return false;
        }

        // Check packet rate
        if (!checkPacketRate(playerId, currentTime)) {
            handleViolation(player, "PacketRate");
            return false;
        }

        // Check for printer/schematica
        if (isPrinterMode(packet)) {
            printerModePlayers.put(playerId, PRINTER_MODE_PACKET_THRESHOLD);
        }

        // Log packet if enabled
        if (plugin.getConfigManager().isPacketLoggingEnabled()) {
            logPacket(player, packet);
        }

        return true;
    }

    private boolean isNettyCrasher(Object packet) {
        // Check for common Netty crasher patterns
        String packetName = packet.getClass().getSimpleName();
        int packetSize = packet.toString().length();

        // Check for oversized packets
        if (packetSize > MAX_PACKET_SIZE) {
            return true;
        }

        // Check for specific crasher packets
        if (packetName.contains("CustomPayload") ||
                packetName.contains("TabComplete") ||
                packetName.contains("WindowClick")) {

            // Additional checks for these packet types
            try {
                // Check for malicious payload data
                if (packetName.contains("CustomPayload")) {
                    Object data = packet.getClass().getMethod("getData").invoke(packet);
                    if (data != null && data.toString().length() > 1000) {
                        return true;
                    }
                }
            } catch (Exception e) {
                // If we can't check the packet, assume it's safe
                return false;
            }
        }

        return false;
    }

    private boolean checkPacketRate(UUID playerId, long currentTime) {
        AtomicInteger violations = packetViolations.computeIfAbsent(playerId.toString(), k -> new AtomicInteger(0));
        Long lastTime = lastPacketTime.get(playerId);

        if (lastTime != null) {
            long timeDiff = currentTime - lastTime;
            if (timeDiff < PACKET_TIMEOUT) {
                // Increment violation counter
                if (violations.incrementAndGet() > VIOLATION_THRESHOLD) {
                    return false;
                }
            } else {
                // Reset violation counter if enough time has passed
                violations.set(0);
            }
        }

        lastPacketTime.put(playerId, currentTime);
        return true;
    }

    private boolean isPrinterMode(Object packet) {
        // Check for printer/schematica patterns
        String packetName = packet.getClass().getSimpleName();

        if (packetName.contains("BlockPlace") ||
                packetName.contains("BlockDig") ||
                packetName.contains("UseItem")) {

            try {
                // Check for rapid block placement
                if (packetName.contains("BlockPlace")) {
                    Object face = packet.getClass().getMethod("getFace").invoke(packet);
                    if (face != null && face.toString().equals("UP")) {
                        return true;
                    }
                }
            } catch (Exception e) {
                // If we can't check the packet, assume it's safe
                return false;
            }
        }

        return false;
    }

    private void handleViolation(Player player, String type) {
        UUID playerId = player.getUniqueId();
        PlayerSecurityData data = playerData.get(playerId);

        if (data != null) {
            data.addViolation(type);

            // Check if player should be kicked
            if (data.getViolations(type) >= VIOLATION_THRESHOLD) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.kickPlayer("§cSecurity violation detected: " + type);
                    plugin.getLogger().warning("Kicked " + player.getName() + " for security violation: " + type);
                });
            }
        }
    }

    private void logPacket(Player player, Object packet) {
        // Log packet details to file
        String logMessage = String.format("[%s] %s: %s",
                player.getName(),
                packet.getClass().getSimpleName(),
                packet.toString());

        plugin.getLogger().info(logMessage);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        addNettyHandler(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Clean up player data
        playerData.remove(playerId);
        packetViolations.remove(playerId.toString());
        lastPacketTime.remove(playerId);
        printerModePlayers.remove(playerId);
    }

    public boolean isPrinterModeEnabled(Player player) {
        return printerModePlayers.containsKey(player.getUniqueId());
    }

    public void disablePrinterMode(Player player) {
        printerModePlayers.remove(player.getUniqueId());
    }

    private static class PlayerSecurityData {
        private final Map<String, Integer> violations;

        public PlayerSecurityData() {
            this.violations = new ConcurrentHashMap<>();
        }

        public void addViolation(String type) {
            violations.merge(type, 1, Integer::sum);
        }

        public int getViolations(String type) {
            return violations.getOrDefault(type, 0);
        }
    }
}