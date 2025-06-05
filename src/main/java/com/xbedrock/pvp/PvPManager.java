package com.xbedrock.pvp;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import com.xbedrock.XBedrockPlugin;

public class PvPManager implements Listener {
    private final XBedrockPlugin plugin;
    private PvPMode currentMode;

    public enum PvPMode {
        LEGACY_1_8("1.8"),
        MODERN("Modern");

        private final String name;

        PvPMode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public PvPManager(XBedrockPlugin plugin) {
        this.plugin = plugin;
        this.currentMode = PvPMode.MODERN; // Default to modern PvP
    }

    public void setPvPMode(PvPMode mode) {
        this.currentMode = mode;
        plugin.getLogger().info("PvP mode set to: " + mode.getName());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }

        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        switch (currentMode) {
            case LEGACY_1_8:
                handleLegacyPvP(event, attacker, victim);
                break;
            case MODERN:
                handleModernPvP(event, attacker, victim);
                break;
        }
    }

    private void handleLegacyPvP(EntityDamageByEntityEvent event, Player attacker, Player victim) {
        // 1.8 PvP mechanics
        double damage = event.getDamage();

        // Apply 1.8 combat mechanics
        if (attacker.isSprinting()) {
            damage *= 1.5; // Sprint multiplier
        }

        // Apply 1.8 cooldown mechanics
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon != null) {
            // Implement 1.8 attack cooldown
            double cooldown = getAttackCooldown(attacker);
            damage *= (0.2 + (cooldown * cooldown * 0.8));
        }

        event.setDamage(damage);
    }

    private void handleModernPvP(EntityDamageByEntityEvent event, Player attacker, Player victim) {
        // Modern PvP mechanics (1.9+)
        double damage = event.getDamage();

        // Apply modern combat mechanics
        if (attacker.isSprinting()) {
            damage *= 1.3; // Modern sprint multiplier
        }

        // Apply modern cooldown mechanics
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon != null) {
            // Implement modern attack cooldown
            double cooldown = getAttackCooldown(attacker);
            damage *= cooldown;
        }

        event.setDamage(damage);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (currentMode == PvPMode.LEGACY_1_8) {
            // Reset cooldown when switching items in 1.8
            Player player = event.getPlayer();
            resetAttackCooldown(player);
        }
    }

    private double getAttackCooldown(Player player) {
        // Get the attack cooldown based on the current PvP mode
        if (currentMode == PvPMode.LEGACY_1_8) {
            return 1.0; // No cooldown in 1.8
        } else {
            // Modern cooldown calculation
            return player.getAttackCooldown();
        }
    }

    private void resetAttackCooldown(Player player) {
        // Reset the attack cooldown
        if (currentMode == PvPMode.LEGACY_1_8) {
            // No cooldown to reset in 1.8
            return;
        }
        // Modern cooldown reset
        player.setAttackCooldown(0);
    }

    public PvPMode getCurrentMode() {
        return currentMode;
    }
}