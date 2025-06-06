package com.xbedrock.config;

import com.xbedrock.XBedrockPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final XBedrockPlugin plugin;
    private final Map<String, Boolean> featureToggles;
    private final Map<String, String> apiEndpoints;
    private final Map<String, String> apiKeys;
    private FileConfiguration config;

    public ConfigManager(XBedrockPlugin plugin) {
        this.plugin = plugin;
        this.featureToggles = new HashMap<>();
        this.apiEndpoints = new HashMap<>();
        this.apiKeys = new HashMap<>();
        loadConfig();
    }

    public void loadConfig() {
        // Create config file if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        loadFeatureToggles();
        loadApiConfigs();
    }

    private void loadFeatureToggles() {
        featureToggles.put("cosmetics", config.getBoolean("features.cosmetics.enabled", true));
        featureToggles.put("webstore", config.getBoolean("features.webstore.enabled", true));
        featureToggles.put("roblox", config.getBoolean("features.roblox.enabled", true));
        featureToggles.put("browser", config.getBoolean("features.browser.enabled", false));
    }

    private void loadApiConfigs() {
        // Roblox API
        apiEndpoints.put("roblox", config.getString("api.roblox.endpoint", "https://api.roblox.com"));
        apiKeys.put("roblox", config.getString("api.roblox.key", ""));

        // Webstore API
        apiEndpoints.put("webstore", config.getString("api.webstore.endpoint", "https://your-webstore.com/api"));
        apiKeys.put("webstore", config.getString("api.webstore.key", ""));

        // Browser API
        apiEndpoints.put("browser", config.getString("api.browser.endpoint", "https://your-browser-api.com"));
        apiKeys.put("browser", config.getString("api.browser.key", ""));
    }

    public void saveConfig() {
        try {
            // Save feature toggles
            for (Map.Entry<String, Boolean> entry : featureToggles.entrySet()) {
                config.set("features." + entry.getKey() + ".enabled", entry.getValue());
            }

            // Save API configs
            for (Map.Entry<String, String> entry : apiEndpoints.entrySet()) {
                config.set("api." + entry.getKey() + ".endpoint", entry.getValue());
            }
            for (Map.Entry<String, String> entry : apiKeys.entrySet()) {
                config.set("api." + entry.getKey() + ".key", entry.getValue());
            }

            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save config: " + e.getMessage());
        }
    }

    public boolean isFeatureEnabled(String feature) {
        return featureToggles.getOrDefault(feature, false);
    }

    public void setFeatureEnabled(String feature, boolean enabled) {
        featureToggles.put(feature, enabled);
        saveConfig();
    }

    public String getApiEndpoint(String service) {
        return getConfig().getString("api." + service + ".endpoint", "");
    }

    public String getApiKey(String service) {
        return getConfig().getString("api." + service + ".key", "");
    }

    public String getWebhookSecret(String service) {
        return getConfig().getString("api." + service + ".webhook-secret", "");
    }

    public void setApiEndpoint(String service, String endpoint) {
        apiEndpoints.put(service, endpoint);
        saveConfig();
    }

    public void setApiKey(String service, String key) {
        apiKeys.put(service, key);
        saveConfig();
    }

    public void reload() {
        loadConfig();
    }

    public boolean isPacketLoggingEnabled() {
        return getConfig().getBoolean("security.logging.enabled", false);
    }
}