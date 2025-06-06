package com.xbedrock.connection;

import org.bukkit.entity.Player;
import java.util.Objects;
import java.util.UUID;

public class BedrockConnection {
    private final UUID uuid;
    private final String username;
    private final String deviceId;
    private final String deviceModel;
    private final String deviceOS;
    private final String clientVersion;
    private final String language;
    private final boolean isPremium;
    private boolean connected;
    private long connectionTime;
    private int ping;
    private String lastWorld;
    private double lastX, lastY, lastZ;
    private float lastYaw, lastPitch;

    public BedrockConnection(UUID uuid, String username, String deviceId, String deviceModel,
            String deviceOS, String clientVersion, String language, boolean isPremium) {
        this.uuid = Objects.requireNonNull(uuid, "UUID cannot be null");
        this.username = Objects.requireNonNull(username, "Username cannot be null");
        this.deviceId = Objects.requireNonNull(deviceId, "Device ID cannot be null");
        this.deviceModel = Objects.requireNonNull(deviceModel, "Device model cannot be null");
        this.deviceOS = Objects.requireNonNull(deviceOS, "Device OS cannot be null");
        this.clientVersion = Objects.requireNonNull(clientVersion, "Client version cannot be null");
        this.language = Objects.requireNonNull(language, "Language cannot be null");
        this.isPremium = isPremium;
        this.connected = false;
        this.connectionTime = System.currentTimeMillis();
        this.ping = -1;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public String getDeviceOS() {
        return deviceOS;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
        if (connected) {
            this.connectionTime = System.currentTimeMillis();
        }
    }

    public long getConnectionTime() {
        return connectionTime;
    }

    public long getConnectionDuration() {
        return System.currentTimeMillis() - connectionTime;
    }

    public int getPing() {
        return ping;
    }

    public void setPing(int ping) {
        this.ping = Math.max(0, ping);
    }

    public void updatePosition(String world, double x, double y, double z, float yaw, float pitch) {
        this.lastWorld = world;
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
        this.lastYaw = yaw;
        this.lastPitch = pitch;
    }

    public String getLastWorld() {
        return lastWorld;
    }

    public double getLastX() {
        return lastX;
    }

    public double getLastY() {
        return lastY;
    }

    public double getLastZ() {
        return lastZ;
    }

    public float getLastYaw() {
        return lastYaw;
    }

    public float getLastPitch() {
        return lastPitch;
    }

    public void disconnect(Player player) {
        if (player != null && player.isOnline()) {
            player.kickPlayer("§cBedrock connection closed");
        }
        this.connected = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BedrockConnection that = (BedrockConnection) o;
        return uuid.equals(that.uuid) && deviceId.equals(that.deviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, deviceId);
    }

    @Override
    public String toString() {
        return String.format("BedrockConnection{uuid=%s, username='%s', device='%s', version='%s', connected=%s}",
                uuid, username, deviceModel, clientVersion, connected);
    }
}