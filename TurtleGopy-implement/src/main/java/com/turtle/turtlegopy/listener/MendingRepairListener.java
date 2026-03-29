package com.turtle.turtlegopy.listener;

import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MendingRepairListener implements Listener {

    private final TurtleGopyCore core;
    // Cooldown to prevent spam (500ms)
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public MendingRepairListener(TurtleGopyCore core) {
        this.core = core;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Check if feature is enabled
        if (!core.getPlugin().getConfig().getBoolean("mending-repair.enabled", true)) return;

        // Only right click (air or block)
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();

        // Must be sneaking (shift)
        if (!player.isSneaking()) return;

        // Check permission
        String permission = core.getPlugin().getConfig().getString("mending-repair.permission", "");
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) return;

        // Cooldown check
        long now = System.currentTimeMillis();
        Long lastUse = cooldowns.get(player.getUniqueId());
        if (lastUse != null && (now - lastUse) < 500) return;

        // Get item in main hand
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || item.getAmount() == 0) return;

        // Check if item has Mending
        if (!item.containsEnchantment(Enchantment.MENDING)) return;

        // Check if item is damageable and damaged
        if (!(item.getItemMeta() instanceof Damageable damageable)) return;
        int currentDamage = damageable.getDamage();
        if (currentDamage <= 0) {
            player.sendMessage(core.getMessage("mending-not-damaged"));
            cooldowns.put(player.getUniqueId(), now);
            return;
        }

        // Check player has XP
        int totalXP = getPlayerTotalXP(player);
        if (totalXP <= 0) {
            player.sendMessage(core.getMessage("mending-no-xp"));
            cooldowns.put(player.getUniqueId(), now);
            return;
        }

        // Mending: 1 XP point = 2 durability repaired
        int durabilityPerXP = core.getPlugin().getConfig().getInt("mending-repair.durability-per-xp", 2);
        int xpNeeded = (int) Math.ceil((double) currentDamage / durabilityPerXP);
        int xpToUse = Math.min(xpNeeded, totalXP);
        int durabilityRepaired = xpToUse * durabilityPerXP;

        // Repair the item
        int newDamage = Math.max(0, currentDamage - durabilityRepaired);
        damageable.setDamage(newDamage);
        item.setItemMeta(damageable);

        // Deduct XP
        setPlayerTotalXP(player, totalXP - xpToUse);

        // Effects
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 1.5f);

        // Send message
        boolean fullyRepaired = newDamage == 0;
        String messageKey = fullyRepaired ? "mending-fully-repaired" : "mending-partially-repaired";
        player.sendMessage(core.getMessage(messageKey)
                .replace("{xp}", String.valueOf(xpToUse))
                .replace("{durability}", String.valueOf(durabilityRepaired)));

        cooldowns.put(player.getUniqueId(), now);

        // Cancel the event to prevent other interactions
        event.setCancelled(true);
    }

    /**
     * Calculate total XP points from player's level and progress.
     */
    private int getPlayerTotalXP(Player player) {
        int level = player.getLevel();
        float progress = player.getExp();

        int totalXP = 0;

        // XP required for each level range
        for (int i = 0; i < level; i++) {
            totalXP += getXPForLevel(i);
        }

        // Add partial level progress
        totalXP += Math.round(progress * getXPForLevel(level));

        return totalXP;
    }

    /**
     * Set player's total XP points.
     */
    private void setPlayerTotalXP(Player player, int totalXP) {
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);

        if (totalXP <= 0) return;

        // Calculate level and remaining XP
        int level = 0;
        int remaining = totalXP;

        while (remaining >= getXPForLevel(level)) {
            remaining -= getXPForLevel(level);
            level++;
        }

        player.setLevel(level);
        int xpForCurrentLevel = getXPForLevel(level);
        if (xpForCurrentLevel > 0) {
            player.setExp((float) remaining / xpForCurrentLevel);
        }
        player.setTotalExperience(totalXP);
    }

    /**
     * Get XP required to complete a specific level.
     * Based on Minecraft wiki formulas.
     */
    private int getXPForLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }
}
