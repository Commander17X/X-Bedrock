package com.xbedrock.browser;

import org.bukkit.entity.Player;
import org.bukkit.Location;

public class BrowserPlayer {
    private final Player player;
    private String browserUsername;
    private String minecraftUsername;
    private Location lastLocation;
    private boolean isOnline;
    private String skinData;
    private String browserSessionId;

    public BrowserPlayer(Player player) {
        this.player = player;
        this.minecraftUsername = player.getName();
        this.isOnline = true;
        this.lastLocation = player.getLocation();
    }

    public Player getPlayer() {
        return player;
    }

    public String getBrowserUsername() {
        return browserUsername;
    }

    public void setBrowserUsername(String browserUsername) {
        this.browserUsername = browserUsername;
    }

    public String getMinecraftUsername() {
        return minecraftUsername;
    }

    public void setMinecraftUsername(String minecraftUsername) {
        this.minecraftUsername = minecraftUsername;
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public String getSkinData() {
        return skinData;
    }

    public void setSkinData(String skinData) {
        this.skinData = skinData;
    }

    public String getBrowserSessionId() {
        return browserSessionId;
    }

    public void setBrowserSessionId(String browserSessionId) {
        this.browserSessionId = browserSessionId;
    }

    public void updatePosition(Location location) {
        this.lastLocation = location;
    }

    public boolean hasMoved() {
        if (lastLocation == null || player == null)
            return false;

        Location currentLocation = player.getLocation();
        return !lastLocation.getWorld().equals(currentLocation.getWorld()) ||
                lastLocation.getX() != currentLocation.getX() ||
                lastLocation.getY() != currentLocation.getY() ||
                lastLocation.getZ() != currentLocation.getZ() ||
                lastLocation.getYaw() != currentLocation.getYaw() ||
                lastLocation.getPitch() != currentLocation.getPitch();
    }

    public void syncWithBrowser() {
        if (!isOnline || player == null)
            return;

        // Update position
        updatePosition(player.getLocation());

        // TODO: Implement additional synchronization logic
        // This could include:
        // - Inventory sync
        // - Health sync
        // - Effects sync
        // - Chat sync
        // - etc.
    }
}