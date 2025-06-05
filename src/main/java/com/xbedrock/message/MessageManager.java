package com.xbedrock.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import com.xbedrock.XBedrockPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManager {
    private final XBedrockPlugin plugin;
    private final MiniMessage miniMessage;
    private final Map<String, String> messages;
    private static final Pattern RGB_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");

    public MessageManager(XBedrockPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.messages = new HashMap<>();
        loadMessages();
    }

    private void loadMessages() {
        FileConfiguration config = plugin.getConfig();

        // Load default messages
        messages.put("welcome", "<gradient:#00ff00:#0000ff>Welcome to the server!</gradient>");
        messages.put("connecting", "<gray>Connecting to Java server...</gray>");
        messages.put("connected", "<green>Successfully connected to the server!");
        messages.put("disconnected", "<red>Disconnected from the server.");
        messages.put("invalid_ip", "<red>Invalid IP address! Please check your connection settings.");
        messages.put("too_many_attempts", "<red>Too many connection attempts. Please try again later.");
        messages.put("command_help", "<yellow>Type /help for available commands");
        messages.put("server_full", "<red>The server is full!");
        messages.put("maintenance", "<red>The server is currently under maintenance.");
        messages.put("version_mismatch", "<red>Your client version is not supported.");
        messages.put("auth_failed", "<red>Authentication failed. Please try again.");
        messages.put("rate_limited", "<red>You are being rate limited. Please wait before trying again.");

        // Load custom messages from config
        if (config.contains("messages")) {
            for (String key : config.getConfigurationSection("messages").getKeys(false)) {
                messages.put(key, config.getString("messages." + key));
            }
        }
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "<red>Message not found: " + key);
    }

    public Component parseMessage(String message) {
        // Convert RGB color codes to MiniMessage format
        message = convertRGBToMiniMessage(message);
        return miniMessage.deserialize(message);
    }

    private String convertRGBToMiniMessage(String message) {
        Matcher matcher = RGB_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexColor = matcher.group(1);
            matcher.appendReplacement(buffer, "<#" + hexColor + ">");
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    public String convertToLegacy(String message) {
        Component component = parseMessage(message);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public void reloadMessages() {
        messages.clear();
        loadMessages();
    }

    public void setMessage(String key, String message) {
        messages.put(key, message);
        // Save to config
        plugin.getConfig().set("messages." + key, message);
        plugin.saveConfig();
    }
}