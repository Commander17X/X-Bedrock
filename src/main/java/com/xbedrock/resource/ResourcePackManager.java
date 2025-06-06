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
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResourcePackManager {
    private final XBedrockPlugin plugin;
    private final Map<String, ResourcePack> resourcePacks;
    private final Map<UUID, String> activePacks;
    private final File resourcePackFolder;
    private final Map<String, ResourcePackInfo> packInfo;
    private static final String MANIFEST_FILE = "manifest.json";
    private static final String PACK_ICON = "pack_icon.png";

    public ResourcePackManager(XBedrockPlugin plugin) {
        this.plugin = plugin;
        this.resourcePacks = new HashMap<>();
        this.activePacks = new ConcurrentHashMap<>();
        this.packInfo = new HashMap<>();
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
                ResourcePackInfo info = extractPackInfo(pack);
                if (info != null) {
                    ResourcePack resourcePack = createResourcePack(pack, info);
                    resourcePacks.put(pack.getName(), resourcePack);
                    packInfo.put(pack.getName(), info);
                    plugin.getLogger()
                            .info("Loaded resource pack: " + pack.getName() + " (Version: " + info.getVersion() + ")");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to load resource pack: " + pack.getName());
                e.printStackTrace();
            }
        }
    }

    private ResourcePackInfo extractPackInfo(File packFile) throws IOException {
        try (ZipFile zip = new ZipFile(packFile)) {
            ZipEntry manifestEntry = zip.getEntry(MANIFEST_FILE);
            if (manifestEntry == null) {
                plugin.getLogger().warning("No manifest.json found in " + packFile.getName());
                return null;
            }

            String manifest = new String(zip.getInputStream(manifestEntry).readAllBytes());
            // Parse manifest.json to get pack info
            // This is a simplified version - you should use proper JSON parsing
            String version = extractVersion(manifest);
            String name = extractName(manifest);
            String description = extractDescription(manifest);
            int format = extractFormat(manifest);

            return new ResourcePackInfo(name, version, description, format);
        }
    }

    private ResourcePack createResourcePack(File packFile, ResourcePackInfo info) throws IOException {
        byte[] content = Files.readAllBytes(packFile.toPath());
        byte[] icon = extractPackIcon(packFile);

        return ResourcePack.builder()
                .name(info.getName())
                .uuid(UUID.randomUUID())
                .version(info.getVersion())
                .size(packFile.length())
                .content(content)
                .icon(icon)
                .format(info.getFormat())
                .build();
    }

    private byte[] extractPackIcon(File packFile) throws IOException {
        try (ZipFile zip = new ZipFile(packFile)) {
            ZipEntry iconEntry = zip.getEntry(PACK_ICON);
            if (iconEntry != null) {
                return zip.getInputStream(iconEntry).readAllBytes();
            }
        }
        return null;
    }

    public void applyResourcePack(UUID playerUUID, String packName) {
        ResourcePack pack = resourcePacks.get(packName);
        if (pack != null) {
            activePacks.put(playerUUID, packName);
            // Apply the resource pack to the player
            // Implementation depends on your server platform
        }
    }

    public void removeResourcePack(UUID playerUUID) {
        activePacks.remove(playerUUID);
        // Remove the resource pack from the player
        // Implementation depends on your server platform
    }

    public ResourcePackInfo getPackInfo(String packName) {
        return packInfo.get(packName);
    }

    public Map<String, ResourcePackInfo> getAllPackInfo() {
        return new HashMap<>(packInfo);
    }

    // Helper methods for manifest parsing
    private String extractVersion(String manifest) {
        // Implement proper JSON parsing
        return "1.0.0";
    }

    private String extractName(String manifest) {
        // Implement proper JSON parsing
        return "Default Pack";
    }

    private String extractDescription(String manifest) {
        // Implement proper JSON parsing
        return "A resource pack";
    }

    private int extractFormat(String manifest) {
        // Implement proper JSON parsing
        return 1;
    }

    public static class ResourcePackInfo {
        private final String name;
        private final String version;
        private final String description;
        private final int format;

        public ResourcePackInfo(String name, String version, String description, int format) {
            this.name = name;
            this.version = version;
            this.description = description;
            this.format = format;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getDescription() {
            return description;
        }

        public int getFormat() {
            return format;
        }
    }
}