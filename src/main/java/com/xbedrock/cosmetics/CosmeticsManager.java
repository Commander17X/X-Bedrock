package com.xbedrock.cosmetics;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.connection.ConnectionSuccessEvent;
import com.xbedrock.XBedrockPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CosmeticsManager implements Listener {
    private final XBedrockPlugin plugin;
    private final Map<UUID, CosmeticData> playerCosmetics;
    private final File cosmeticsFolder;
    private final Map<String, CosmeticType> registeredCosmetics;

    public CosmeticsManager(XBedrockPlugin plugin) {
        this.plugin = plugin;
        this.playerCosmetics = new HashMap<>();
        this.cosmeticsFolder = new File(plugin.getDataFolder(), "cosmetics");
        this.registeredCosmetics = new HashMap<>();

        if (!cosmeticsFolder.exists()) {
            cosmeticsFolder.mkdirs();
        }

        registerDefaultCosmetics();
    }

    private void registerDefaultCosmetics() {
        // Register default cosmetic types
        registerCosmeticType("ears", new CosmeticType("ears", "Ears", true));
        registerCosmeticType("cape", new CosmeticType("cape", "Cape", true));
        registerCosmeticType("wings", new CosmeticType("wings", "Wings", true));
        registerCosmeticType("hat", new CosmeticType("hat", "Hat", true));
        registerCosmeticType("mask", new CosmeticType("mask", "Mask", true));
    }

    public void registerCosmeticType(String id, CosmeticType type) {
        registeredCosmetics.put(id, type);
        plugin.getLogger().info("Registered cosmetic type: " + type.getName());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isBedrockPlayer(player)) {
            // Load cosmetics for Bedrock player
            loadPlayerCosmetics(player);
        }
    }

    @EventHandler
    public void onConnectionSuccess(ConnectionSuccessEvent event) {
        GeyserConnection connection = event.connection();
        Player player = plugin.getServer().getPlayer(connection.bedrockUuid());
        if (player != null) {
            // Apply cosmetics to Bedrock player
            applyCosmetics(player);
        }
    }

    private void loadPlayerCosmetics(Player player) {
        // Load cosmetics from data file
        File playerFile = new File(cosmeticsFolder, player.getUniqueId().toString() + ".yml");
        if (playerFile.exists()) {
            CosmeticData data = loadCosmeticData(playerFile);
            playerCosmetics.put(player.getUniqueId(), data);
        } else {
            // Create default cosmetic data
            CosmeticData data = new CosmeticData(player.getUniqueId());
            playerCosmetics.put(player.getUniqueId(), data);
            saveCosmeticData(data);
        }
    }

    private void applyCosmetics(Player player) {
        CosmeticData data = playerCosmetics.get(player.getUniqueId());
        if (data != null) {
            // Apply each cosmetic
            for (Map.Entry<String, String> entry : data.getActiveCosmetics().entrySet()) {
                String typeId = entry.getKey();
                String cosmeticId = entry.getValue();
                applyCosmetic(player, typeId, cosmeticId);
            }
        }
    }

    private void applyCosmetic(Player player, String typeId, String cosmeticId) {
        CosmeticType type = registeredCosmetics.get(typeId);
        if (type != null && type.isEnabled()) {
            // Apply the cosmetic to the player
            // This would involve sending packets to the Bedrock client
            sendCosmeticPacket(player, type, cosmeticId);
        }
    }

    private void sendCosmeticPacket(Player player, CosmeticType type, String cosmeticId) {
        GeyserConnection connection = GeyserApi.api().connectionByUuid(player.getUniqueId());
        if (connection != null) {
            // Send cosmetic data to Bedrock client
            // This is a placeholder for the actual packet sending logic
            connection.sendMessage("Applying cosmetic: " + type.getName() + " - " + cosmeticId);
        }
    }

    private boolean isBedrockPlayer(Player player) {
        GeyserConnection connection = GeyserApi.api().connectionByUuid(player.getUniqueId());
        return connection != null;
    }

    private CosmeticData loadCosmeticData(File file) {
        // Load cosmetic data from file
        // This is a placeholder for the actual loading logic
        return new CosmeticData(UUID.fromString(file.getName().replace(".yml", "")));
    }

    private void saveCosmeticData(CosmeticData data) {
        // Save cosmetic data to file
        // This is a placeholder for the actual saving logic
        File file = new File(cosmeticsFolder, data.getPlayerId().toString() + ".yml");
        // Save data to file
    }

    public static class CosmeticType {
        private final String id;
        private final String name;
        private boolean enabled;

        public CosmeticType(String id, String name, boolean enabled) {
            this.id = id;
            this.name = name;
            this.enabled = enabled;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class CosmeticData {
        private final UUID playerId;
        private final Map<String, String> activeCosmetics;

        public CosmeticData(UUID playerId) {
            this.playerId = playerId;
            this.activeCosmetics = new HashMap<>();
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public Map<String, String> getActiveCosmetics() {
            return activeCosmetics;
        }

        public void setCosmetic(String typeId, String cosmeticId) {
            activeCosmetics.put(typeId, cosmeticId);
        }

        public void removeCosmetic(String typeId) {
            activeCosmetics.remove(typeId);
        }
    }
}