package com.xbedrock.cosmetics;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.connection.ConnectionSuccessEvent;
import com.xbedrock.XBedrockPlugin;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CosmeticsManager implements Listener {
    private final XBedrockPlugin plugin;
    private final Set<UUID> enabledPlayers;
    private boolean enabled;
    private final Map<UUID, CosmeticData> playerCosmetics;
    private final File cosmeticsFolder;
    private final Map<String, CosmeticType> registeredCosmetics;

    public CosmeticsManager(XBedrockPlugin plugin) {
        this.plugin = plugin;
        this.enabledPlayers = new HashSet<>();
        this.enabled = plugin.getConfigManager().getConfig().getBoolean("features.cosmetics.enabled", true);
        this.playerCosmetics = new HashMap<>();
        this.cosmeticsFolder = new File(plugin.getDataFolder(), "cosmetics");
        this.registeredCosmetics = new HashMap<>();

        if (!cosmeticsFolder.exists()) {
            cosmeticsFolder.mkdirs();
        }

        registerDefaultCosmetics();

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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

    public void enableCosmetics(Player player) {
        if (!enabled)
            return;
        enabledPlayers.add(player.getUniqueId());
        // Apply cosmetics to player
        applyCosmetics(player);
    }

    public void disableCosmetics(Player player) {
        enabledPlayers.remove(player.getUniqueId());
        // Remove cosmetics from player
        removeCosmetics(player);
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

    private void removeCosmetics(Player player) {
        // Remove all active cosmetics
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

        // Remove armor stands used for cosmetics
        player.getWorld().getEntities().stream()
                .filter(entity -> entity.getType().name().equals("ARMOR_STAND"))
                .filter(entity -> entity.getCustomName() != null
                        && entity.getCustomName().startsWith("cosmetic_" + player.getUniqueId()))
                .forEach(entity -> entity.remove());

        // Remove particles
        player.getWorld().getPlayers().forEach(p -> {
            if (p.canSee(player)) {
                p.spigot().stopParticles(player);
            }
        });

        // Remove custom player data
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data != null) {
            data.setCosmeticsEnabled(false);
            plugin.getPlayerDataManager().savePlayerData(data);
        }

        // Notify player
        player.sendMessage("§aAll cosmetics have been removed.");
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (enabledPlayers.contains(player.getUniqueId())) {
            applyCosmetics(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (enabledPlayers.contains(player.getUniqueId())) {
            removeCosmetics(player);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            // Disable cosmetics for all players
            plugin.getServer().getOnlinePlayers().forEach(this::disableCosmetics);
        }
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