package com.xbedrock.player;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class PlayerData {
    private final Player player;
    private boolean isBedrockPlayer;
    private String webstoreId;
    private long lastLogin;
    private final Map<String, Long> purchases;
    private final Map<String, String> cosmetics;
    private String robloxUsername;
    private String robloxId;
    private String browserUsername;
    private String browserSessionId;

    public PlayerData(Player player) {
        this.player = player;
        this.isBedrockPlayer = false;
        this.webstoreId = "";
        this.lastLogin = System.currentTimeMillis();
        this.purchases = new HashMap<>();
        this.cosmetics = new HashMap<>();
        this.robloxUsername = "";
        this.robloxId = "";
        this.browserUsername = "";
        this.browserSessionId = "";
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isBedrockPlayer() {
        return isBedrockPlayer;
    }

    public void setBedrockPlayer(boolean bedrockPlayer) {
        isBedrockPlayer = bedrockPlayer;
    }

    public String getWebstoreId() {
        return webstoreId;
    }

    public void setWebstoreId(String webstoreId) {
        this.webstoreId = webstoreId;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Map<String, Long> getPurchases() {
        return purchases;
    }

    public void addPurchase(String itemId, long purchaseTime) {
        purchases.put(itemId, purchaseTime);
    }

    public void removePurchase(String itemId) {
        purchases.remove(itemId);
    }

    public boolean hasPurchase(String itemId) {
        return purchases.containsKey(itemId);
    }

    public Map<String, String> getCosmetics() {
        return cosmetics;
    }

    public void setCosmetic(String type, String cosmeticId) {
        cosmetics.put(type, cosmeticId);
    }

    public void removeCosmetic(String type) {
        cosmetics.remove(type);
    }

    public String getCosmetic(String type) {
        return cosmetics.get(type);
    }

    public String getRobloxUsername() {
        return robloxUsername;
    }

    public void setRobloxUsername(String robloxUsername) {
        this.robloxUsername = robloxUsername;
    }

    public String getRobloxId() {
        return robloxId;
    }

    public void setRobloxId(String robloxId) {
        this.robloxId = robloxId;
    }

    public String getBrowserUsername() {
        return browserUsername;
    }

    public void setBrowserUsername(String browserUsername) {
        this.browserUsername = browserUsername;
    }

    public String getBrowserSessionId() {
        return browserSessionId;
    }

    public void setBrowserSessionId(String browserSessionId) {
        this.browserSessionId = browserSessionId;
    }

    public boolean isLinkedWithRoblox() {
        return robloxUsername != null && !robloxUsername.isEmpty();
    }

    public boolean isLinkedWithBrowser() {
        return browserUsername != null && !browserUsername.isEmpty();
    }

    public String getDisplayName() {
        if (isBedrockPlayer) {
            return "!-<" + player.getName() + ">";
        }
        return player.getName();
    }
}