package com.xbedrock.compatibility;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import com.xbedrock.XBedrockPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CompatibilityManager implements Listener {
    private final XBedrockPlugin plugin;
    private final Map<UUID, ItemUseState> itemUseStates;
    private final Map<UUID, Long> lastItemUse;

    public CompatibilityManager(XBedrockPlugin plugin) {
        this.plugin = plugin;
        this.itemUseStates = new HashMap<>();
        this.lastItemUse = new HashMap<>();
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (isBedrockPlayer(player)) {
            // Reset inventory on world switch
            player.getInventory().clear();
            player.updateInventory();
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isBedrockPlayer(player)) {
            // Fix non-vanilla block breaking
            if (!isVanillaBlock(event.getBlock().getType())) {
                event.setCancelled(true);
                // Handle custom block breaking
                handleCustomBlockBreak(event);
            }
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (isBedrockPlayer(player)) {
            // Don't send item use when player is already using item
            ItemUseState state = itemUseStates.get(player.getUniqueId());
            if (state != null && state.isUsingItem()) {
                event.setCancelled(true);
                return;
            }

            // Update item use state
            updateItemUseState(player, event.getNewSlot());
        }
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (isBedrockPlayer(player) && event.isSneaking()) {
            // Don't attempt to use shield while climbing down scaffolding
            if (player.isClimbing()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (isBedrockPlayer(player)) {
                // Fix MCPL to Geyser holder sets
                if (event.getInventory().getHolder() instanceof GeyserHolder) {
                    handleGeyserInventoryClick(event);
                }
            }
        }
    }

    private boolean isBedrockPlayer(Player player) {
        GeyserConnection connection = GeyserApi.api().connectionByUuid(player.getUniqueId());
        return connection != null;
    }

    private boolean isVanillaBlock(org.bukkit.Material material) {
        // Check if the block is a vanilla block
        return material.isBlock() && !material.name().startsWith("CUSTOM_");
    }

    private void handleCustomBlockBreak(BlockBreakEvent event) {
        // Implement custom block breaking logic
        Player player = event.getPlayer();
        org.bukkit.block.Block block = event.getBlock();

        // Send block break animation
        player.sendBlockChange(block.getLocation(), block.getBlockData());

        // Handle custom block breaking
        // This is where you would implement your custom block breaking logic
    }

    private void updateItemUseState(Player player, int slot) {
        ItemUseState state = itemUseStates.computeIfAbsent(player.getUniqueId(), k -> new ItemUseState());
        state.setCurrentSlot(slot);
        state.setUsingItem(false);
    }

    private void handleGeyserInventoryClick(InventoryClickEvent event) {
        // Fix MCPL to Geyser holder sets
        if (event.getInventory().getHolder() instanceof GeyserHolder) {
            GeyserHolder holder = (GeyserHolder) event.getInventory().getHolder();
            // Handle Geyser-specific inventory clicks
            holder.handleClick(event);
        }
    }

    private static class ItemUseState {
        private int currentSlot;
        private boolean usingItem;
        private long lastUseTime;

        public ItemUseState() {
            this.currentSlot = 0;
            this.usingItem = false;
            this.lastUseTime = 0;
        }

        public int getCurrentSlot() {
            return currentSlot;
        }

        public void setCurrentSlot(int currentSlot) {
            this.currentSlot = currentSlot;
        }

        public boolean isUsingItem() {
            return usingItem;
        }

        public void setUsingItem(boolean usingItem) {
            this.usingItem = usingItem;
            if (usingItem) {
                this.lastUseTime = System.currentTimeMillis();
            }
        }

        public long getLastUseTime() {
            return lastUseTime;
        }
    }

    private static class GeyserHolder implements org.bukkit.inventory.InventoryHolder {
        private final org.bukkit.inventory.Inventory inventory;

        public GeyserHolder(org.bukkit.inventory.Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public org.bukkit.inventory.Inventory getInventory() {
            return inventory;
        }

        public void handleClick(InventoryClickEvent event) {
            // Handle Geyser-specific inventory clicks
            // This is where you would implement your custom inventory click handling
        }
    }
}