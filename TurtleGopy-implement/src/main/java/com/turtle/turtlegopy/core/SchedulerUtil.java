package com.turtle.turtlegopy.core;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

/**
 * Utility class that handles scheduling tasks compatible with both
 * Spigot/Paper and Folia servers.
 */
public class SchedulerUtil {

    private static boolean isFolia;

    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }
    }

    public static boolean isFolia() {
        return isFolia;
    }

    /**
     * Run a task on the appropriate thread for the given player.
     * On Folia: uses the entity's scheduler (region thread).
     * On Spigot/Paper: uses the global scheduler (main thread).
     */
    public static void runEntityTask(JavaPlugin plugin, Player player, Runnable task) {
        if (isFolia) {
            player.getScheduler().run(plugin, scheduledTask -> task.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Run a task with a delay on the appropriate thread for the given player.
     * On Folia: uses the entity's scheduler.
     * On Spigot/Paper: uses the global scheduler.
     */
    public static void runEntityTaskLater(JavaPlugin plugin, Player player, Runnable task, long delayTicks) {
        if (isFolia) {
            player.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), null, delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * Run a task on the global region (main thread on non-Folia).
     * On Folia: uses the global region scheduler.
     * On Spigot/Paper: uses the global scheduler.
     */
    public static void runGlobalTask(JavaPlugin plugin, Runnable task) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Run a task on the global region with a delay.
     */
    public static void runGlobalTaskLater(JavaPlugin plugin, Runnable task, long delayTicks) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * Cancel a scheduled task by taskId. Only works on non-Folia.
     * On Folia, use the ScheduledTask object directly.
     */
    public static void cancelTask(JavaPlugin plugin, int taskId) {
        if (!isFolia) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    /**
     * Run an async task (same on both Folia and non-Folia).
     */
    public static void runAsync(JavaPlugin plugin, Runnable task) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }
}
