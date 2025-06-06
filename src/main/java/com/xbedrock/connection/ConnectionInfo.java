package com.xbedrock.connection;

import java.util.UUID;

public class ConnectionInfo {
    private final UUID uuid;
    private final String username;
    private final String address;
    private final long queueTime;
    private String deviceId;
    private String deviceModel;
    private String deviceOS;
    private String clientVersion;
    private String language;
    private boolean isPremium;

    public ConnectionInfo(UUID uuid, String username, String address, long queueTime) {
        this.uuid = uuid;
        this.username = username;
        this.address = address;
        this.queueTime = queueTime;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public String getAddress() {
        return address;
    }

    public long getQueueTime() {
        return queueTime;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getDeviceOS() {
        return deviceOS;
    }

    public void setDeviceOS(String deviceOS) {
        this.deviceOS = deviceOS;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public void setPremium(boolean premium) {
        isPremium = premium;
    }
}