package com.xbedrock.player;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private final Map<String, Long> purchases;
    private String prefix;
    private boolean cosmeticsEnabled;
    private final Map<String, Object> customData;

    public PlayerData(Player player) {
        this.uuid = player.getUniqueId();
        this.purchases = new HashMap<>();
        this.prefix = "";
        this.cosmeticsEnabled = true;
        this.customData = new HashMap<>();
    }

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.purchases = new HashMap<>();
        this.prefix = "";
        this.cosmeticsEnabled = true;
        this.customData = new HashMap<>();
    }

    public UUID getUuid() {
        return uuid;
    }

    public Map<String, Long> getPurchases() {
        return purchases;
    }

    public void addPurchase(String itemId) {
        purchases.put(itemId, System.currentTimeMillis());
    }

    public void removePurchase(String itemId) {
        purchases.remove(itemId);
    }

    public boolean hasPurchase(String itemId) {
        return purchases.containsKey(itemId);
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public boolean isCosmeticsEnabled() {
        return cosmeticsEnabled;
    }

    public void setCosmeticsEnabled(boolean enabled) {
        this.cosmeticsEnabled = enabled;
    }

    public Object getCustomValue(String key) {
        return customData.get(key);
    }

    public void setCustomValue(String key, Object value) {
        customData.put(key, value);
    }

    public void removeCustomValue(String key) {
        customData.remove(key);
    }

    public Map<String, Object> getCustomData() {
        return customData;
    }

    // Bedrock-specific getters
    public String getBedrockUsername() {
        return (String) customData.getOrDefault("bedrock_username", "");
    }

    public String getDeviceId() {
        return (String) customData.getOrDefault("device_id", "");
    }

    public String getDeviceModel() {
        return (String) customData.getOrDefault("device_model", "");
    }

    public String getDeviceOS() {
        return (String) customData.getOrDefault("device_os", "");
    }

    public String getClientVersion() {
        return (String) customData.getOrDefault("client_version", "");
    }

    public String getLanguage() {
        return (String) customData.getOrDefault("language", "en_US");
    }

    public boolean isPremium() {
        return (boolean) customData.getOrDefault("is_premium", false);
    }
}