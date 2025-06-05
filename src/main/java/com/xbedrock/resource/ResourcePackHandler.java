package com.xbedrock.resource;

import org.bukkit.configuration.file.FileConfiguration;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.resourcepack.ResourcePack;
import org.geysermc.geyser.api.resourcepack.ResourcePackManager;
import com.xbedrock.XBedrockPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ResourcePackHandler {
    private final XBedrockPlugin plugin;
    private final Map<String, ResourcePack> resourcePacks;
    private final File resourcePackFolder;

    public ResourcePackHandler(XBedrockPlugin plugin) {
        this.plugin = plugin;
        this.resourcePacks = new HashMap<>();
        this.resourcePackFolder = new File(plugin.getDataFolder(), "resourcepacks");

        if (!resourcePackFolder.exists()) {
            resourcePackFolder.mkdirs();
        }
    }

    public void loadResourcePacks() {
        File[] packs = resourcePackFolder.listFiles((dir, name) -> name.endsWith(".zip") || name.endsWith(".mcpack"));
        if (packs == null)
            return;

        for (File pack : packs) {
            try {
                // Fix ResourcePack builder issue
                ResourcePack resourcePack = createResourcePack(pack);
                resourcePacks.put(pack.getName(), resourcePack);
                plugin.getLogger().info("Loaded resource pack: " + pack.getName());
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to load resource pack: " + pack.getName());
                e.printStackTrace();
            }
        }
    }

    private ResourcePack createResourcePack(File packFile) throws IOException {
        // Fix ResourcePack builder issue by using the correct builder method
        return ResourcePack.builder()
                .name(packFile.getName())
                .uuid(UUID.randomUUID())
                .version("1.0.0")
                .size(packFile.length())
                .content(Files.readAllBytes(packFile.toPath()))
                .build();
    }

    public void registerResourcePacks() {
        ResourcePackManager geyserResourcePackManager = GeyserApi.api().resourcePackManager();

        for (ResourcePack pack : resourcePacks.values()) {
            try {
                geyserResourcePackManager.registerResourcePack(pack);
                plugin.getLogger().info("Registered resource pack: " + pack.name());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to register resource pack: " + pack.name());
                e.printStackTrace();
            }
        }
    }

    public void addResourcePack(File packFile) {
        try {
            ResourcePack resourcePack = createResourcePack(packFile);
            resourcePacks.put(packFile.getName(), resourcePack);
            GeyserApi.api().resourcePackManager().registerResourcePack(resourcePack);
            plugin.getLogger().info("Added new resource pack: " + packFile.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to add resource pack: " + packFile.getName());
            e.printStackTrace();
        }
    }

    public void removeResourcePack(String packName) {
        ResourcePack pack = resourcePacks.remove(packName);
        if (pack != null) {
            try {
                GeyserApi.api().resourcePackManager().unregisterResourcePack(pack);
                plugin.getLogger().info("Removed resource pack: " + packName);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to remove resource pack: " + packName);
                e.printStackTrace();
            }
        }
    }

    public Map<String, ResourcePack> getResourcePacks() {
        return resourcePacks;
    }

    public void reloadResourcePacks() {
        // Unregister all resource packs
        for (ResourcePack pack : resourcePacks.values()) {
            try {
                GeyserApi.api().resourcePackManager().unregisterResourcePack(pack);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to unregister resource pack: " + pack.name());
                e.printStackTrace();
            }
        }

        // Clear the resource packs map
        resourcePacks.clear();

        // Reload resource packs
        loadResourcePacks();
        registerResourcePacks();
    }
}