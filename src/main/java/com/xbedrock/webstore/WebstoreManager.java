package com.xbedrock.webstore;

import com.xbedrock.XBedrockPlugin;
import com.xbedrock.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class WebstoreManager implements Listener {
    private final XBedrockPlugin plugin;
    private final Map<UUID, String> playerPrefixes;
    private final Map<UUID, PurchaseHistory> purchaseHistory;
    private final Map<String, StoreItem> storeItems;
    private final File dataFolder;
    private final HttpClient httpClient;
    private final JSONParser jsonParser;
    private boolean enabled;
    private String apiEndpoint;
    private String apiKey;
    private String webhookSecret;
    private static final int MAX_PURCHASE_HISTORY = 100;
    private static final String HASH_ALGORITHM = "SHA-256";

    public WebstoreManager(XBedrockPlugin plugin) {
        this.plugin = plugin;
        this.playerPrefixes = new ConcurrentHashMap<>();
        this.purchaseHistory = new ConcurrentHashMap<>();
        this.storeItems = new HashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "webstore");
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
        this.jsonParser = new JSONParser();
        this.enabled = false;

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Load store items
        loadStoreItems();
    }

    private void loadStoreItems() {
        File itemsFile = new File(dataFolder, "items.json");
        if (!itemsFile.exists()) {
            // Create default items
            createDefaultItems();
            return;
        }

        try (FileReader reader = new FileReader(itemsFile)) {
            JSONObject items = (JSONObject) jsonParser.parse(reader);
            for (Object key : items.keySet()) {
                String itemId = (String) key;
                JSONObject itemData = (JSONObject) items.get(itemId);
                storeItems.put(itemId, new StoreItem(
                        itemId,
                        (String) itemData.get("name"),
                        (String) itemData.get("description"),
                        ((Number) itemData.get("price")).doubleValue(),
                        (String) itemData.get("category"),
                        (Map<String, Object>) itemData.get("features")));
            }
        } catch (IOException | ParseException e) {
            plugin.getLogger().severe("Failed to load store items: " + e.getMessage());
        }
    }

    private void createDefaultItems() {
        // Create default store items
        storeItems.put("vip", new StoreItem(
                "vip",
                "VIP Rank",
                "Get access to VIP features and cosmetics",
                1000.0,
                "ranks",
                Map.of(
                        "prefix", "§6[VIP]",
                        "cosmetics", true,
                        "chat_color", "§6")));

        storeItems.put("premium", new StoreItem(
                "premium",
                "Premium Rank",
                "Get access to all VIP features plus exclusive content",
                5000.0,
                "ranks",
                Map.of(
                        "prefix", "§b[Premium]",
                        "cosmetics", true,
                        "chat_color", "§b",
                        "special_effects", true)));

        // Save default items
        saveStoreItems();
    }

    private void saveStoreItems() {
        File itemsFile = new File(dataFolder, "items.json");
        JSONObject items = new JSONObject();

        for (StoreItem item : storeItems.values()) {
            JSONObject itemData = new JSONObject();
            itemData.put("name", item.getName());
            itemData.put("description", item.getDescription());
            itemData.put("price", item.getPrice());
            itemData.put("category", item.getCategory());
            itemData.put("features", item.getFeatures());
            items.put(item.getId(), itemData);
        }

        try (FileWriter writer = new FileWriter(itemsFile)) {
            writer.write(items.toJSONString());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save store items: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled)
            return;

        Player player = event.getPlayer();
        loadPlayerData(player);
    }

    private void loadPlayerData(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null)
            return;

        File playerFile = new File(dataFolder, player.getUniqueId().toString() + ".json");
        if (!playerFile.exists())
            return;

        try (FileReader reader = new FileReader(playerFile)) {
            JSONObject data = (JSONObject) jsonParser.parse(reader);

            // Load prefix
            String prefix = (String) data.get("prefix");
            if (prefix != null) {
                playerPrefixes.put(player.getUniqueId(), prefix);
            }

            // Load purchase history
            JSONObject history = (JSONObject) data.get("purchaseHistory");
            if (history != null) {
                PurchaseHistory purchases = new PurchaseHistory();
                for (Object key : history.keySet()) {
                    String itemId = (String) key;
                    JSONObject purchaseData = (JSONObject) history.get(itemId);
                    purchases.addPurchase(new Purchase(
                            itemId,
                            (long) purchaseData.get("timestamp"),
                            (String) purchaseData.get("transactionId")));
                }
                purchaseHistory.put(player.getUniqueId(), purchases);
            }
        } catch (IOException | ParseException e) {
            plugin.getLogger().severe("Failed to load webstore data for " + player.getName() + ": " + e.getMessage());
        }
    }

    public void handlePurchase(Player player, String itemId) {
        if (!enabled)
            return;

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null)
            return;

        StoreItem item = storeItems.get(itemId);
        if (item == null) {
            player.sendMessage("§cInvalid item ID: " + itemId);
            return;
        }

        // Create purchase request
        JSONObject purchaseData = new JSONObject();
        purchaseData.put("playerId", player.getUniqueId().toString());
        purchaseData.put("itemId", itemId);
        purchaseData.put("price", item.getPrice());
        purchaseData.put("currency", "USD");
        purchaseData.put("timestamp", System.currentTimeMillis());

        // Send purchase request to webstore API
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiEndpoint + "/purchase"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(purchaseData.toJSONString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject responseData = (JSONObject) jsonParser.parse(response.body());
                String transactionId = (String) responseData.get("transactionId");

                // Add purchase to history
                PurchaseHistory history = purchaseHistory.computeIfAbsent(player.getUniqueId(),
                        k -> new PurchaseHistory());
                history.addPurchase(new Purchase(itemId, System.currentTimeMillis(), transactionId));

                // Apply purchase effects
                applyPurchase(player, item);

                // Save player data
                savePlayerData(player);

                player.sendMessage("§aSuccessfully purchased " + item.getName() + "!");
            } else {
                player.sendMessage("§cFailed to process purchase. Please try again later.");
                plugin.getLogger()
                        .severe("Failed to process purchase for " + player.getName() + ": " + response.body());
            }
        } catch (Exception e) {
            player.sendMessage("§cAn error occurred while processing your purchase.");
            plugin.getLogger().severe("Error processing purchase for " + player.getName() + ": " + e.getMessage());
        }
    }

    private void applyPurchase(Player player, StoreItem item) {
        // Apply item features
        Map<String, Object> features = item.getFeatures();

        // Apply prefix if present
        if (features.containsKey("prefix")) {
            String prefix = (String) features.get("prefix");
            playerPrefixes.put(player.getUniqueId(), prefix);
        }

        // Apply cosmetics if enabled
        if (features.containsKey("cosmetics") && (boolean) features.get("cosmetics")) {
            plugin.getCosmeticsManager().enableCosmetics(player);
        }

        // Apply special effects if present
        if (features.containsKey("special_effects") && (boolean) features.get("special_effects")) {
            // Implement special effects
        }

        // Give physical items if present
        if (features.containsKey("items")) {
            Map<String, Object> items = (Map<String, Object>) features.get("items");
            for (Map.Entry<String, Object> entry : items.entrySet()) {
                String itemId = entry.getKey();
                int amount = ((Number) entry.getValue()).intValue();
                // Give item to player
                // Implementation depends on your item system
            }
        }
    }

    public void handleRefund(Player player, String itemId) {
        if (!enabled)
            return;

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null)
            return;

        PurchaseHistory history = purchaseHistory.get(player.getUniqueId());
        if (history == null)
            return;

        Purchase purchase = history.getPurchase(itemId);
        if (purchase == null)
            return;

        // Create refund request
        JSONObject refundData = new JSONObject();
        refundData.put("transactionId", purchase.getTransactionId());
        refundData.put("reason", "Player request");
        refundData.put("timestamp", System.currentTimeMillis());

        // Send refund request to webstore API
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiEndpoint + "/refund"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(refundData.toJSONString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Remove purchase from history
                history.removePurchase(itemId);

                // Remove purchase effects
                removePurchase(player, itemId);

                // Save player data
                savePlayerData(player);

                player.sendMessage("§aSuccessfully refunded your purchase!");
            } else {
                player.sendMessage("§cFailed to process refund. Please try again later.");
                plugin.getLogger().severe("Failed to process refund for " + player.getName() + ": " + response.body());
            }
        } catch (Exception e) {
            player.sendMessage("§cAn error occurred while processing your refund.");
            plugin.getLogger().severe("Error processing refund for " + player.getName() + ": " + e.getMessage());
        }
    }

    private void removePurchase(Player player, String itemId) {
        StoreItem item = storeItems.get(itemId);
        if (item == null)
            return;

        // Remove item features
        Map<String, Object> features = item.getFeatures();

        // Remove prefix if present
        if (features.containsKey("prefix")) {
            playerPrefixes.remove(player.getUniqueId());
        }

        // Disable cosmetics if present
        if (features.containsKey("cosmetics") && (boolean) features.get("cosmetics")) {
            plugin.getCosmeticsManager().disableCosmetics(player);
        }

        // Remove special effects if present
        if (features.containsKey("special_effects") && (boolean) features.get("special_effects")) {
            // Remove special effects
        }

        // Remove physical items if present
        if (features.containsKey("items")) {
            Map<String, Object> items = (Map<String, Object>) features.get("items");
            for (String itemId2 : items.keySet()) {
                // Remove item from player
                // Implementation depends on your item system
            }
        }
    }

    private void savePlayerData(Player player) {
        File playerFile = new File(dataFolder, player.getUniqueId().toString() + ".json");
        JSONObject data = new JSONObject();

        // Save prefix
        String prefix = playerPrefixes.get(player.getUniqueId());
        if (prefix != null) {
            data.put("prefix", prefix);
        }

        // Save purchase history
        PurchaseHistory history = purchaseHistory.get(player.getUniqueId());
        if (history != null) {
            JSONObject historyData = new JSONObject();
            for (Purchase purchase : history.getPurchases()) {
                JSONObject purchaseData = new JSONObject();
                purchaseData.put("timestamp", purchase.getTimestamp());
                purchaseData.put("transactionId", purchase.getTransactionId());
                historyData.put(purchase.getItemId(), purchaseData);
            }
            data.put("purchaseHistory", historyData);
        }

        try (FileWriter writer = new FileWriter(playerFile)) {
            writer.write(data.toJSONString());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save webstore data for " + player.getName() + ": " + e.getMessage());
        }
    }

    public void setPrefix(Player player, String prefix) {
        if (!enabled)
            return;

        playerPrefixes.put(player.getUniqueId(), prefix);
        savePlayerData(player);
    }

    public String getPrefix(Player player) {
        return playerPrefixes.getOrDefault(player.getUniqueId(), "!-<" + player.getName() + ">");
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void reload() {
        // Reload configuration
        apiEndpoint = plugin.getConfigManager().getApiEndpoint("webstore");
        apiKey = plugin.getConfigManager().getApiKey("webstore");
        webhookSecret = plugin.getConfigManager().getWebhookSecret("webstore");

        // Reload store items
        loadStoreItems();
    }

    public int getConnectedPlayers() {
        return playerPrefixes.size();
    }

    public void syncWithWebstore(Player player) {
        if (!enabled)
            return;

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null)
            return;

        // TODO: Implement actual webstore sync
        // This would sync:
        // - Purchase history
        // - Active items
        // - Player status
        // - etc.
    }

    public static class StoreItem {
        private final String id;
        private final String name;
        private final String description;
        private final double price;
        private final String category;
        private final Map<String, Object> features;

        public StoreItem(String id, String name, String description, double price, String category,
                Map<String, Object> features) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.category = category;
            this.features = features;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public double getPrice() {
            return price;
        }

        public String getCategory() {
            return category;
        }

        public Map<String, Object> getFeatures() {
            return features;
        }
    }

    public static class Purchase {
        private final String itemId;
        private final long timestamp;
        private final String transactionId;

        public Purchase(String itemId, long timestamp, String transactionId) {
            this.itemId = itemId;
            this.timestamp = timestamp;
            this.transactionId = transactionId;
        }

        public String getItemId() {
            return itemId;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getTransactionId() {
            return transactionId;
        }
    }

    public static class PurchaseHistory {
        private final Map<String, Purchase> purchases;

        public PurchaseHistory() {
            this.purchases = new HashMap<>();
        }

        public void addPurchase(Purchase purchase) {
            purchases.put(purchase.getItemId(), purchase);
            // Limit history size
            if (purchases.size() > MAX_PURCHASE_HISTORY) {
                // Remove oldest purchase
                purchases.entrySet().stream()
                        .min(Map.Entry.comparingByValue((p1, p2) -> Long.compare(p1.getTimestamp(), p2.getTimestamp())))
                        .ifPresent(entry -> purchases.remove(entry.getKey()));
            }
        }

        public void removePurchase(String itemId) {
            purchases.remove(itemId);
        }

        public Purchase getPurchase(String itemId) {
            return purchases.get(itemId);
        }

        public java.util.Collection<Purchase> getPurchases() {
            return purchases.values();
        }
    }
}