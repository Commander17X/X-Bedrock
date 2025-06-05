package com.xbedrock.player;

import com.xbedrock.XBedrockPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager implements Listener {
    private final XBedrockPlugin plugin;
    private final Map<UUID, PlayerData> playerData;
    private final File dataFolder;
    private final JSONParser jsonParser;

    public PlayerDataManager(XBedrockPlugin plugin) {
        this.plugin = plugin;
        this.playerData = new ConcurrentHashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.jsonParser = new JSONParser();

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        loadPlayerData(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        savePlayerData(player);
    }

    private void loadPlayerData(Player player) {
        File playerFile = new File(dataFolder, player.getUniqueId().toString() + ".json");
        if (!playerFile.exists()) {
            // Create new player data
            PlayerData data = new PlayerData(player);
            playerData.put(player.getUniqueId(), data);
            return;
        }

        try (FileReader reader = new FileReader(playerFile)) {
            JSONObject json = (JSONObject) jsonParser.parse(reader);
            PlayerData data = new PlayerData(player);

            // Load basic data
            data.setBedrockPlayer(json.getBoolean("isBedrock"));
            data.setWebstoreId(json.getString("webstoreId"));
            data.setLastLogin(json.getLong("lastLogin"));

            // Load purchases
            JSONObject purchases = (JSONObject) json.get("purchases");
            if (purchases != null) {
                for (Object key : purchases.keySet()) {
                    String itemId = (String) key;
                    long purchaseTime = (long) purchases.get(itemId);
                    data.addPurchase(itemId, purchaseTime);
                }
            }

            // Load cosmetics
            JSONObject cosmetics = (JSONObject) json.get("cosmetics");
            if (cosmetics != null) {
                for (Object key : cosmetics.keySet()) {
                    String type = (String) key;
                    String cosmeticId = (String) cosmetics.get(type);
                    data.setCosmetic(type, cosmeticId);
                }
            }

            // Load Roblox data
            JSONObject roblox = (JSONObject) json.get("roblox");
            if (roblox != null) {
                data.setRobloxUsername((String) roblox.get("username"));
                data.setRobloxId((String) roblox.get("id"));
            }

            // Load Browser data
            JSONObject browser = (JSONObject) json.get("browser");
            if (browser != null) {
                data.setBrowserUsername((String) browser.get("username"));
                data.setBrowserSessionId((String) browser.get("sessionId"));
            }

            playerData.put(player.getUniqueId(), data);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load player data for " + player.getName() + ": " + e.getMessage());
            // Create new player data if loading fails
            PlayerData data = new PlayerData(player);
            playerData.put(player.getUniqueId(), data);
        }
    }

    private void savePlayerData(Player player) {
        PlayerData data = playerData.get(player.getUniqueId());
        if (data == null)
            return;

        File playerFile = new File(dataFolder, player.getUniqueId().toString() + ".json");
        try (FileWriter writer = new FileWriter(playerFile)) {
            JSONObject json = new JSONObject();

            // Save basic data
            json.put("isBedrock", data.isBedrockPlayer());
            json.put("webstoreId", data.getWebstoreId());
            json.put("lastLogin", data.getLastLogin());

            // Save purchases
            JSONObject purchases = new JSONObject();
            for (Map.Entry<String, Long> entry : data.getPurchases().entrySet()) {
                purchases.put(entry.getKey(), entry.getValue());
            }
            json.put("purchases", purchases);

            // Save cosmetics
            JSONObject cosmetics = new JSONObject();
            for (Map.Entry<String, String> entry : data.getCosmetics().entrySet()) {
                cosmetics.put(entry.getKey(), entry.getValue());
            }
            json.put("cosmetics", cosmetics);

            // Save Roblox data
            JSONObject roblox = new JSONObject();
            roblox.put("username", data.getRobloxUsername());
            roblox.put("id", data.getRobloxId());
            json.put("roblox", roblox);

            // Save Browser data
            JSONObject browser = new JSONObject();
            browser.put("username", data.getBrowserUsername());
            browser.put("sessionId", data.getBrowserSessionId());
            json.put("browser", browser);

            writer.write(json.toJSONString());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save player data for " + player.getName() + ": " + e.getMessage());
        }
    }

    public PlayerData getPlayerData(Player player) {
        return playerData.get(player.getUniqueId());
    }

    public void updatePlayerData(Player player, PlayerData data) {
        playerData.put(player.getUniqueId(), data);
        savePlayerData(player);
    }

    public void handleWebstorePurchase(Player player, String itemId) {
        PlayerData data = getPlayerData(player);
        if (data != null) {
            data.addPurchase(itemId, System.currentTimeMillis());
            savePlayerData(player);
        }
    }

    public void handleWebstoreRefund(Player player, String itemId) {
        PlayerData data = getPlayerData(player);
        if (data != null) {
            data.removePurchase(itemId);
            savePlayerData(player);
        }
    }

    public void syncWithWebstore(Player player) {
        PlayerData data = getPlayerData(player);
        if (data == null)
            return;

        // TODO: Implement actual webstore API call
        // This would sync:
        // - Purchases
        // - Cosmetics
        // - Player status
        // - etc.
    }
}