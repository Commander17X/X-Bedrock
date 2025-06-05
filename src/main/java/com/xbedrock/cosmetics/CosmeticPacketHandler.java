package com.xbedrock.cosmetics;

import org.bukkit.entity.Player;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.network.ProtocolVersion;
import com.xbedrock.XBedrockPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CosmeticPacketHandler {
    private final XBedrockPlugin plugin;
    private final Map<String, byte[]> cosmeticCache;

    public CosmeticPacketHandler(XBedrockPlugin plugin) {
        this.plugin = plugin;
        this.cosmeticCache = new HashMap<>();
    }

    public void sendCosmeticData(Player player, String typeId, String cosmeticId) {
        GeyserConnection connection = GeyserApi.api().connectionByUuid(player.getUniqueId());
        if (connection == null)
            return;

        try {
            byte[] packetData = createCosmeticPacket(typeId, cosmeticId);
            sendPacket(connection, packetData);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to send cosmetic data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private byte[] createCosmeticPacket(String typeId, String cosmeticId) throws IOException {
        // Check cache first
        String cacheKey = typeId + ":" + cosmeticId;
        if (cosmeticCache.containsKey(cacheKey)) {
            return cosmeticCache.get(cacheKey);
        }

        // Create packet data
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Write packet header
        dos.writeByte(0x50); // Custom packet ID for cosmetics
        dos.writeUTF(typeId);
        dos.writeUTF(cosmeticId);

        // Write cosmetic data
        writeCosmeticData(dos, typeId, cosmeticId);

        byte[] packetData = baos.toByteArray();
        cosmeticCache.put(cacheKey, packetData);
        return packetData;
    }

    private void writeCosmeticData(DataOutputStream dos, String typeId, String cosmeticId) throws IOException {
        switch (typeId) {
            case "ears":
                writeEarsData(dos, cosmeticId);
                break;
            case "cape":
                writeCapeData(dos, cosmeticId);
                break;
            case "wings":
                writeWingsData(dos, cosmeticId);
                break;
            case "hat":
                writeHatData(dos, cosmeticId);
                break;
            case "mask":
                writeMaskData(dos, cosmeticId);
                break;
            default:
                throw new IOException("Unknown cosmetic type: " + typeId);
        }
    }

    private void writeEarsData(DataOutputStream dos, String cosmeticId) throws IOException {
        // Write ears-specific data
        dos.writeUTF(cosmeticId); // Ears texture ID
        dos.writeFloat(1.0f); // Scale
        dos.writeBoolean(true); // Visible
    }

    private void writeCapeData(DataOutputStream dos, String cosmeticId) throws IOException {
        // Write cape-specific data
        dos.writeUTF(cosmeticId); // Cape texture ID
        dos.writeFloat(1.0f); // Scale
        dos.writeBoolean(true); // Visible
    }

    private void writeWingsData(DataOutputStream dos, String cosmeticId) throws IOException {
        // Write wings-specific data
        dos.writeUTF(cosmeticId); // Wings texture ID
        dos.writeFloat(1.0f); // Scale
        dos.writeBoolean(true); // Visible
    }

    private void writeHatData(DataOutputStream dos, String cosmeticId) throws IOException {
        // Write hat-specific data
        dos.writeUTF(cosmeticId); // Hat texture ID
        dos.writeFloat(1.0f); // Scale
        dos.writeBoolean(true); // Visible
    }

    private void writeMaskData(DataOutputStream dos, String cosmeticId) throws IOException {
        // Write mask-specific data
        dos.writeUTF(cosmeticId); // Mask texture ID
        dos.writeFloat(1.0f); // Scale
        dos.writeBoolean(true); // Visible
    }

    private void sendPacket(GeyserConnection connection, byte[] packetData) {
        // Send packet to Bedrock client
        connection.sendPacket(packetData);
    }

    public void clearCache() {
        cosmeticCache.clear();
    }

    public void removeFromCache(String typeId, String cosmeticId) {
        cosmeticCache.remove(typeId + ":" + cosmeticId);
    }
}