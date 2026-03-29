package com.turtle.turtlegopy.manager;

import java.util.ArrayList;
import java.util.List;

import com.turtle.turtlegopy.core.SchedulerUtil;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import lombok.Getter;

/**
 * Manages automatic broadcast messages that repeat on configurable intervals.
 * Messages are grouped into blocks - each broadcast sends an entire block at once.
 */
@Getter
public class BroadcastManager {

    private final TurtleGopyCore core;
    private final List<Object> activeTasks = new ArrayList<>();

    public BroadcastManager(TurtleGopyCore core) {
        this.core = core;
    }

    public void start() {
        stop();

        if (!core.getPlugin().getConfig().getBoolean("broadcast.enabled", false)) {
            core.getPlugin().getLogger().info("Broadcast đã bị tắt trong config.");
            return;
        }

        ConfigurationSection groupsSection = core.getPlugin().getConfig().getConfigurationSection("broadcast.groups");
        if (groupsSection == null) {
            core.getPlugin().getLogger().warning("Không tìm thấy nhóm broadcast nào trong config.");
            return;
        }

        for (String groupKey : groupsSection.getKeys(false)) {
            ConfigurationSection group = groupsSection.getConfigurationSection(groupKey);
            if (group == null) continue;

            boolean groupEnabled = group.getBoolean("enabled", true);
            if (!groupEnabled) continue;

            int intervalMinutes = group.getInt("interval", 5);
            String permission = group.getString("permission", "");
            boolean randomOrder = group.getBoolean("random", false);

            if (intervalMinutes <= 0) continue;

            // Load message blocks from numbered sections
            ConfigurationSection messagesSection = group.getConfigurationSection("messages");
            if (messagesSection == null) continue;

            List<List<String>> messageBlocks = new ArrayList<>();
            for (String blockKey : messagesSection.getKeys(false)) {
                List<String> block = messagesSection.getStringList(blockKey);
                if (!block.isEmpty()) {
                    messageBlocks.add(block);
                }
            }

            if (messageBlocks.isEmpty()) continue;

            long intervalTicks = Math.max(1L, intervalMinutes * 60L * 20L);
            final int[] currentIndex = {0};

            Runnable broadcastTask = () -> {
                List<String> block;
                if (randomOrder) {
                    int idx = (int) (Math.random() * messageBlocks.size());
                    block = messageBlocks.get(idx);
                } else {
                    block = messageBlocks.get(currentIndex[0]);
                    currentIndex[0] = (currentIndex[0] + 1) % messageBlocks.size();
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (permission.isEmpty() || player.hasPermission(permission)) {
                        for (String line : block) {
                            player.sendMessage(core.colorize(line));
                        }
                    }
                }
            };

            // Schedule repeating task
            if (SchedulerUtil.isFolia()) {
                io.papermc.paper.threadedregions.scheduler.ScheduledTask task =
                        Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                                core.getPlugin(),
                                scheduledTask -> broadcastTask.run(),
                                intervalTicks,
                                intervalTicks
                        );
                activeTasks.add(task);
            } else {
                int taskId = Bukkit.getScheduler().runTaskTimer(
                        core.getPlugin(),
                        broadcastTask,
                        intervalTicks,
                        intervalTicks
                ).getTaskId();
                activeTasks.add(taskId);
            }

            core.getPlugin().getLogger().info(
                    "Đã khởi tạo broadcast '" + groupKey + "' - " +
                    messageBlocks.size() + " cụm tin nhắn, lặp mỗi " + intervalMinutes + " phút.");
        }
    }

    public void stop() {
        for (Object task : activeTasks) {
            if (SchedulerUtil.isFolia()) {
                if (task instanceof io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask) {
                    foliaTask.cancel();
                }
            } else {
                if (task instanceof Integer taskId) {
                    Bukkit.getScheduler().cancelTask(taskId);
                }
            }
        }
        activeTasks.clear();
    }

    public void reload() {
        stop();
        start();
    }
}
