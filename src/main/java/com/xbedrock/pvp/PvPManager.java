package com.xbedrock.pvp;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.xbedrock.XBedrockPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PvPManager implements Listener {
    private final XBedrockPlugin plugin;
    private PvPMode currentMode;
    private final Map<UUID, CombatData> combatData;
    private final Map<UUID, Long> lastAttackTime;
    private final Map<UUID, Double> attackCooldown;
    private static final long ATTACK_COOLDOWN = 600; // 0.6 seconds in milliseconds
    private static final double CRITICAL_MULTIPLIER = 1.5;
    private static final double SWEEPING_MULTIPLIER = 0.5;

    public enum PvPMode {
        LEGACY_1_8("1.8"),
        MODERN("Modern"),
        HYBRID("Hybrid");

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
        this.currentMode = PvPMode.MODERN;
        this.combatData = new ConcurrentHashMap<>();
        this.lastAttackTime = new ConcurrentHashMap<>();
        this.attackCooldown = new ConcurrentHashMap<>();
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

        // Update combat data
        updateCombatData(attacker, victim);

        switch (currentMode) {
            case LEGACY_1_8:
                handleLegacyPvP(event, attacker, victim);
                break;
            case MODERN:
                handleModernPvP(event, attacker, victim);
                break;
            case HYBRID:
                handleHybridPvP(event, attacker, victim);
                break;
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        if (currentMode == PvPMode.MODERN) {
            updateAttackCooldown(event.getPlayer(), event.getNewSlot());
        }
    }

    @EventHandler
    public void onPlayerSprint(PlayerToggleSprintEvent event) {
        if (currentMode == PvPMode.MODERN && event.isSprinting()) {
            Player player = event.getPlayer();
            CombatData data = combatData.get(player.getUniqueId());
            if (data != null) {
                data.setSprinting(true);
            }
        }
    }

    private void handleLegacyPvP(EntityDamageByEntityEvent event, Player attacker, Player victim) {
        // 1.8 PvP mechanics
        double damage = event.getDamage();

        // Apply critical hits
        if (attacker.isSprinting() && !attacker.isOnGround()) {
            damage *= CRITICAL_MULTIPLIER;
        }

        // Apply sweeping edge
        if (attacker.isSprinting() && attacker.isOnGround()) {
            damage *= SWEEPING_MULTIPLIER;
        }

        event.setDamage(damage);
    }

    private void handleModernPvP(EntityDamageByEntityEvent event, Player attacker, Player victim) {
        // Modern PvP mechanics (1.9+)
        double damage = event.getDamage();
        double cooldown = attackCooldown.getOrDefault(attacker.getUniqueId(), 1.0);

        // Apply attack cooldown
        damage *= cooldown;

        // Apply critical hits
        if (attacker.isSprinting() && !attacker.isOnGround() && cooldown > 0.9) {
            damage *= CRITICAL_MULTIPLIER;
        }

        // Apply sweeping edge
        if (attacker.isSprinting() && attacker.isOnGround() && cooldown > 0.9) {
            damage *= SWEEPING_MULTIPLIER;
        }

        event.setDamage(damage);
    }

    private void handleHybridPvP(EntityDamageByEntityEvent event, Player attacker, Player victim) {
        // Hybrid PvP mechanics (mix of 1.8 and modern)
        double damage = event.getDamage();
        double cooldown = attackCooldown.getOrDefault(attacker.getUniqueId(), 1.0);

        // Apply reduced cooldown effect
        damage *= Math.max(0.5, cooldown);

        // Apply critical hits (easier to get)
        if (attacker.isSprinting() && !attacker.isOnGround() && cooldown > 0.7) {
            damage *= CRITICAL_MULTIPLIER;
        }

        // Apply sweeping edge (easier to get)
        if (attacker.isSprinting() && attacker.isOnGround() && cooldown > 0.7) {
            damage *= SWEEPING_MULTIPLIER;
        }

        event.setDamage(damage);
    }

    private void updateCombatData(Player attacker, Player victim) {
        long currentTime = System.currentTimeMillis();

        // Update attacker data
        CombatData attackerData = combatData.computeIfAbsent(attacker.getUniqueId(), k -> new CombatData());
        attackerData.setLastAttackTime(currentTime);
        attackerData.setLastVictim(victim.getUniqueId());

        // Update victim data
        CombatData victimData = combatData.computeIfAbsent(victim.getUniqueId(), k -> new CombatData());
        victimData.setLastAttackedTime(currentTime);
        victimData.setLastAttacker(attacker.getUniqueId());

        // Apply combat tag effect
        victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0, false, false));
    }

    private void updateAttackCooldown(Player player, int newSlot) {
        long currentTime = System.currentTimeMillis();
        long lastAttack = lastAttackTime.getOrDefault(player.getUniqueId(), 0L);
        long timeSinceLastAttack = currentTime - lastAttack;

        double cooldown = Math.min(1.0, (double) timeSinceLastAttack / ATTACK_COOLDOWN);
        attackCooldown.put(player.getUniqueId(), cooldown);
    }

    private static class CombatData {
        private long lastAttackTime;
        private long lastAttackedTime;
        private UUID lastVictim;
        private UUID lastAttacker;
        private boolean sprinting;

        public CombatData() {
            this.lastAttackTime = 0;
            this.lastAttackedTime = 0;
            this.sprinting = false;
        }

        // Getters and setters
        public long getLastAttackTime() {
            return lastAttackTime;
        }

        public void setLastAttackTime(long lastAttackTime) {
            this.lastAttackTime = lastAttackTime;
        }

        public long getLastAttackedTime() {
            return lastAttackedTime;
        }

        public void setLastAttackedTime(long lastAttackedTime) {
            this.lastAttackedTime = lastAttackedTime;
        }

        public UUID getLastVictim() {
            return lastVictim;
        }

        public void setLastVictim(UUID lastVictim) {
            this.lastVictim = lastVictim;
        }

        public UUID getLastAttacker() {
            return lastAttacker;
        }

        public void setLastAttacker(UUID lastAttacker) {
            this.lastAttacker = lastAttacker;
        }

        public boolean isSprinting() {
            return sprinting;
        }

        public void setSprinting(boolean sprinting) {
            this.sprinting = sprinting;
        }
    }
}