package com.xbedrock.resource;

import org.bukkit.configuration.file.FileConfiguration;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.resourcepack.ResourcePack;
import org.geysermc.geyser.api.resourcepack.ResourcePackManager;
import com.xbedrock.XBedrockPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ResourcePackManager {
    private final XBedrockPlugin plugin;
    private final Map<String, ResourcePack> resourcePacks;
    private final File resourcePackFolder;

    public ResourcePackManager(XBedrockPlugin plugin) {
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
                ResourcePack resourcePack = ResourcePack.builder()
                        .name(pack.getName())
                        .uuid(UUID.randomUUID())
                        .version("1.0.0")
                        .size(pack.length())
                        .content(Files.readAllBytes(pack.toPath()))
                        .build();

                resourcePacks.put(pack.getName(), resourcePack);
                plugin.getLogger().info("Loaded resource pack: " + pack.getName());
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to load resource pack: " + pack.getName());
                e.printStackTrace();
            }
        }
    }

    public void registerResourcePacks() {
        ResourcePackManager geyserResourcePackManager = GeyserApi.api().resourcePackManager();

        for (ResourcePack pack : resourcePacks.values()) {
            geyserResourcePackManager.registerResourcePack(pack);
            plugin.getLogger().info("Registered resource pack: " + pack.name());
        }
    }

    public void addResourcePack(File packFile) {
        try {
            ResourcePack resourcePack = ResourcePack.builder()
                    .name(packFile.getName())
                    .uuid(UUID.randomUUID())
                    .version("1.0.0")
                    .size(packFile.length())
                    .content(Files.readAllBytes(packFile.toPath()))
                    .build();

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
            GeyserApi.api().resourcePackManager().unregisterResourcePack(pack);
            plugin.getLogger().info("Removed resource pack: " + packName);
        }
    }

    public Map<String, ResourcePack> getResourcePacks() {
        return resourcePacks;
    }
}