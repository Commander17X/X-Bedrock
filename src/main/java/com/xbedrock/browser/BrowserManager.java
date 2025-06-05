package com.xbedrock.browser;

import com.xbedrock.XBedrockPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BrowserManager implements Listener {
    private final XBedrockPlugin plugin;
    private final Map<UUID, BrowserPlayer> browserPlayers;
    private final File dataFolder;
    private boolean enabled;
    private String apiEndpoint;
    private String apiKey;

    public BrowserManager(XBedrockPlugin plugin) {
        this.plugin = plugin;
        this.browserPlayers = new ConcurrentHashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "browser");
        this.enabled = false;

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Start position sync task
        startPositionSync();
    }

    private void startPositionSync() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!enabled)
                    return;

                for (BrowserPlayer browserPlayer : browserPlayers.values()) {
                    if (browserPlayer.isOnline()) {
                        syncPlayerPosition(browserPlayer);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled)
            return;

        Player player = event.getPlayer();
        BrowserPlayer browserPlayer = new BrowserPlayer(player);
        browserPlayers.put(player.getUniqueId(), browserPlayer);

        // Send initial position
        syncPlayerPosition(browserPlayer);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!enabled)
            return;

        browserPlayers.remove(event.getPlayer().getUniqueId());
    }

    public void syncPlayerPosition(BrowserPlayer browserPlayer) {
        if (!enabled)
            return;

        Player player = browserPlayer.getPlayer();
        if (player == null || !player.isOnline())
            return;

        // Create position data
        Map<String, Object> positionData = new HashMap<>();
        positionData.put("uuid", player.getUniqueId().toString());
        positionData.put("name", player.getName());
        positionData.put("position", String.format("%.2f,%.2f,%.2f",
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ()));
        positionData.put("rotation", String.format("%.2f,%.2f",
                player.getLocation().getYaw(),
                player.getLocation().getPitch()));

        // Send to Browser API
        sendToBrowserAPI(positionData);
    }

    private void sendToBrowserAPI(Map<String, Object> data) {
        // Implementation will depend on your Browser API endpoint
        // This is a placeholder for the actual API call
        try {
            // TODO: Implement actual API call to Browser
            plugin.getLogger().info("Sending position data to Browser: " + data);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to send data to Browser API: " + e.getMessage());
        }
    }

    public void syncAllPlayers() {
        if (!enabled)
            return;

        for (BrowserPlayer browserPlayer : browserPlayers.values()) {
            syncPlayerPosition(browserPlayer);
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void reload() {
        // Reload configuration
        // TODO: Implement configuration reloading
    }

    public int getConnectedBrowserPlayers() {
        return browserPlayers.size();
    }

    public void handleBrowserPlayerJoin(String browserUsername, String minecraftUsername) {
        // Handle Browser player joining
        // This will be called when a Browser player connects to the Minecraft server
        // TODO: Implement Browser player join handling
    }

    public void handleBrowserPlayerQuit(String browserUsername) {
        // Handle Browser player leaving
        // TODO: Implement Browser player quit handling
    }
}