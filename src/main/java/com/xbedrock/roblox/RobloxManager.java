package com.xbedrock.roblox;

import com.xbedrock.XBedrockPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RobloxManager implements Listener {
    private final XBedrockPlugin plugin;
    private final Map<UUID, RobloxPlayer> robloxPlayers;
    private final File dataFolder;
    private final HttpClient httpClient;
    private final JSONParser jsonParser;
    private final ScheduledExecutorService scheduler;
    private boolean enabled;
    private String apiEndpoint;
    private String apiKey;
    private static final int SYNC_INTERVAL = 1; // seconds
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 1000; // milliseconds

    public RobloxManager(XBedrockPlugin plugin) {
        this.plugin = plugin;
        this.robloxPlayers = new ConcurrentHashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "roblox");
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
        this.jsonParser = new JSONParser();
        this.scheduler = Executors.newScheduledThreadPool(1);
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
        scheduler.scheduleAtFixedRate(() -> {
            if (!enabled)
                return;

            for (RobloxPlayer robloxPlayer : robloxPlayers.values()) {
                if (robloxPlayer.isOnline()) {
                    syncPlayerPosition(robloxPlayer);
                }
            }
        }, 0, SYNC_INTERVAL, TimeUnit.SECONDS);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled)
            return;

        Player player = event.getPlayer();
        RobloxPlayer robloxPlayer = new RobloxPlayer(player);
        robloxPlayers.put(player.getUniqueId(), robloxPlayer);

        // Load Roblox data
        loadRobloxData(player);

        // Send initial position
        syncPlayerPosition(robloxPlayer);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!enabled)
            return;

        Player player = event.getPlayer();
        RobloxPlayer robloxPlayer = robloxPlayers.remove(player.getUniqueId());
        if (robloxPlayer != null) {
            saveRobloxData(player);
        }
    }

    private void loadRobloxData(Player player) {
        File playerFile = new File(dataFolder, player.getUniqueId().toString() + ".json");
        if (!playerFile.exists())
            return;

        try (FileReader reader = new FileReader(playerFile)) {
            JSONObject data = (JSONObject) jsonParser.parse(reader);
            RobloxPlayer robloxPlayer = robloxPlayers.get(player.getUniqueId());
            if (robloxPlayer != null) {
                robloxPlayer.setRobloxId((String) data.get("robloxId"));
                robloxPlayer.setLastSyncTime((long) data.get("lastSyncTime"));
                robloxPlayer.setInventory((Map<String, Object>) data.get("inventory"));
            }
        } catch (IOException | ParseException e) {
            plugin.getLogger().severe("Failed to load Roblox data for " + player.getName() + ": " + e.getMessage());
        }
    }

    private void saveRobloxData(Player player) {
        File playerFile = new File(dataFolder, player.getUniqueId().toString() + ".json");
        RobloxPlayer robloxPlayer = robloxPlayers.get(player.getUniqueId());
        if (robloxPlayer == null)
            return;

        JSONObject data = new JSONObject();
        data.put("robloxId", robloxPlayer.getRobloxId());
        data.put("lastSyncTime", robloxPlayer.getLastSyncTime());
        data.put("inventory", robloxPlayer.getInventory());

        try (FileWriter writer = new FileWriter(playerFile)) {
            writer.write(data.toJSONString());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save Roblox data for " + player.getName() + ": " + e.getMessage());
        }
    }

    public void syncPlayerPosition(RobloxPlayer robloxPlayer) {
        if (!enabled)
            return;

        Player player = robloxPlayer.getPlayer();
        if (player == null || !player.isOnline())
            return;

        // Create position data
        JSONObject positionData = new JSONObject();
        positionData.put("uuid", player.getUniqueId().toString());
        positionData.put("name", player.getName());
        positionData.put("position", String.format("%.2f,%.2f,%.2f",
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ()));
        positionData.put("rotation", String.format("%.2f,%.2f",
                player.getLocation().getYaw(),
                player.getLocation().getPitch()));
        positionData.put("world", player.getWorld().getName());
        positionData.put("gameMode", player.getGameMode().name());
        positionData.put("health", player.getHealth());
        positionData.put("food", player.getFoodLevel());

        // Send to Roblox API with retry logic
        sendToRobloxAPIWithRetry(positionData, 0);
    }

    private void sendToRobloxAPIWithRetry(JSONObject data, int retryCount) {
        if (retryCount >= MAX_RETRIES) {
            plugin.getLogger().warning("Failed to send data to Roblox API after " + MAX_RETRIES + " attempts");
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiEndpoint + "/sync/position"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(data.toJSONString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Success
                JSONObject responseData = (JSONObject) jsonParser.parse(response.body());
                handleRobloxResponse(responseData);
            } else if (response.statusCode() >= 500) {
                // Server error, retry
                plugin.getLogger().warning("Roblox API server error, retrying in " + RETRY_DELAY + "ms");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        sendToRobloxAPIWithRetry(data, retryCount + 1);
                    }
                }.runTaskLater(plugin, RETRY_DELAY / 50);
            } else {
                // Client error, log and don't retry
                plugin.getLogger()
                        .severe("Roblox API client error: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to send data to Roblox API: " + e.getMessage());
            // Retry on network errors
            new BukkitRunnable() {
                @Override
                public void run() {
                    sendToRobloxAPIWithRetry(data, retryCount + 1);
                }
            }.runTaskLater(plugin, RETRY_DELAY / 50);
        }
    }

    private void handleRobloxResponse(JSONObject response) {
        // Handle Roblox server response
        if (response.containsKey("players")) {
            JSONObject players = (JSONObject) response.get("players");
            for (Object key : players.keySet()) {
                String robloxId = (String) key;
                JSONObject playerData = (JSONObject) players.get(robloxId);
                updatePlayerFromRoblox(robloxId, playerData);
            }
        }

        if (response.containsKey("events")) {
            JSONObject events = (JSONObject) response.get("events");
            for (Object key : events.keySet()) {
                String eventType = (String) key;
                JSONObject eventData = (JSONObject) events.get(eventType);
                handleRobloxEvent(eventType, eventData);
            }
        }
    }

    private void updatePlayerFromRoblox(String robloxId, JSONObject playerData) {
        // Find the corresponding Minecraft player
        for (RobloxPlayer robloxPlayer : robloxPlayers.values()) {
            if (robloxId.equals(robloxPlayer.getRobloxId())) {
                Player player = robloxPlayer.getPlayer();
                if (player != null && player.isOnline()) {
                    // Update player data from Roblox
                    if (playerData.containsKey("position")) {
                        String[] pos = ((String) playerData.get("position")).split(",");
                        player.teleport(player.getLocation().set(
                                Double.parseDouble(pos[0]),
                                Double.parseDouble(pos[1]),
                                Double.parseDouble(pos[2])));
                    }
                    if (playerData.containsKey("inventory")) {
                        updatePlayerInventory(player, (JSONObject) playerData.get("inventory"));
                    }
                }
                break;
            }
        }
    }

    private void handleRobloxEvent(String eventType, JSONObject eventData) {
        switch (eventType) {
            case "chat":
                handleChatEvent(eventData);
                break;
            case "inventory":
                handleInventoryEvent(eventData);
                break;
            case "teleport":
                handleTeleportEvent(eventData);
                break;
            default:
                plugin.getLogger().warning("Unknown Roblox event type: " + eventType);
        }
    }

    private void handleChatEvent(JSONObject eventData) {
        String robloxId = (String) eventData.get("playerId");
        String message = (String) eventData.get("message");

        // Find the corresponding Minecraft player
        for (RobloxPlayer robloxPlayer : robloxPlayers.values()) {
            if (robloxId.equals(robloxPlayer.getRobloxId())) {
                Player player = robloxPlayer.getPlayer();
                if (player != null && player.isOnline()) {
                    // Broadcast the message to all players
                    plugin.getServer().broadcastMessage(
                            String.format("[Roblox] %s: %s", player.getName(), message));
                }
                break;
            }
        }
    }

    private void handleInventoryEvent(JSONObject eventData) {
        String robloxId = (String) eventData.get("playerId");
        JSONObject inventory = (JSONObject) eventData.get("inventory");

        // Find the corresponding Minecraft player
        for (RobloxPlayer robloxPlayer : robloxPlayers.values()) {
            if (robloxId.equals(robloxPlayer.getRobloxId())) {
                Player player = robloxPlayer.getPlayer();
                if (player != null && player.isOnline()) {
                    updatePlayerInventory(player, inventory);
                }
                break;
            }
        }
    }

    private void handleTeleportEvent(JSONObject eventData) {
        String robloxId = (String) eventData.get("playerId");
        String[] pos = ((String) eventData.get("position")).split(",");

        // Find the corresponding Minecraft player
        for (RobloxPlayer robloxPlayer : robloxPlayers.values()) {
            if (robloxId.equals(robloxPlayer.getRobloxId())) {
                Player player = robloxPlayer.getPlayer();
                if (player != null && player.isOnline()) {
                    player.teleport(player.getLocation().set(
                            Double.parseDouble(pos[0]),
                            Double.parseDouble(pos[1]),
                            Double.parseDouble(pos[2])));
                }
                break;
            }
        }
    }

    private void updatePlayerInventory(Player player, JSONObject inventory) {
        // Update player's inventory based on Roblox data
        // This is a simplified version - you should implement proper item conversion
        player.getInventory().clear();
        for (Object key : inventory.keySet()) {
            String slot = (String) key;
            JSONObject itemData = (JSONObject) inventory.get(slot);
            // Convert Roblox item to Minecraft item and set in inventory
            // Implementation depends on your item conversion logic
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            scheduler.shutdown();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void reload() {
        // Reload configuration
        apiEndpoint = plugin.getConfigManager().getApiEndpoint("roblox");
        apiKey = plugin.getConfigManager().getApiKey("roblox");
    }

    public int getConnectedPlayers() {
        return robloxPlayers.size();
    }

    public void handleRobloxPlayerJoin(String robloxUsername, String minecraftUsername) {
        // Handle Roblox player joining
        // This will be called when a Roblox player connects to the Minecraft server
        // TODO: Implement Roblox player join handling
    }

    public void handleRobloxPlayerQuit(String robloxUsername) {
        // Handle Roblox player leaving
        // TODO: Implement Roblox player quit handling
    }
}