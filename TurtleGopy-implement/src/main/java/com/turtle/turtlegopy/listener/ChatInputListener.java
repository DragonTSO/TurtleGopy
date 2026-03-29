package com.turtle.turtlegopy.listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.turtle.turtlegopy.api.model.BugReport;
import com.turtle.turtlegopy.api.model.Feedback;
import com.turtle.turtlegopy.api.model.SupportTicket;
import com.turtle.turtlegopy.core.SchedulerUtil;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatInputListener implements Listener {

    private static final int MODE_FEEDBACK = 1;
    private static final int MODE_BUGREPORT = 2;
    private static final int MODE_SUPPORT = 3;

    private final TurtleGopyCore core;
    private final Map<UUID, Integer> pendingInput = new ConcurrentHashMap<>();
    private final Map<UUID, Object> timeoutTasks = new ConcurrentHashMap<>();

    public ChatInputListener(TurtleGopyCore core) {
        this.core = core;
    }

    public void startInput(Player player) {
        startInputMode(player, MODE_FEEDBACK);
    }

    public void startBugReportInput(Player player) {
        startInputMode(player, MODE_BUGREPORT);
    }

    public void startSupportInput(Player player) {
        startInputMode(player, MODE_SUPPORT);
    }

    private void startInputMode(Player player, int mode) {
        UUID uuid = player.getUniqueId();
        int timeout = core.getPlugin().getConfig().getInt("chat-input.timeout", 60);

        pendingInput.put(uuid, mode);

        // Cancel existing timeout if any
        cancelTimeout(uuid);

        // Schedule timeout using Folia-compatible scheduler
        if (SchedulerUtil.isFolia()) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask task =
                    player.getScheduler().runDelayed(core.getPlugin(), scheduledTask -> {
                        if (pendingInput.remove(uuid) != null) {
                            Player p = core.getPlugin().getServer().getPlayer(uuid);
                            if (p != null && p.isOnline()) {
                                String msgKey;
                                if (mode == MODE_BUGREPORT) {
                                    msgKey = "bugreport-input-timeout";
                                } else if (mode == MODE_SUPPORT) {
                                    msgKey = "support-input-timeout";
                                } else {
                                    msgKey = "feedback-input-timeout";
                                }
                                p.sendMessage(core.getMessage(msgKey));
                            }
                        }
                        timeoutTasks.remove(uuid);
                    }, null, timeout * 20L);
            timeoutTasks.put(uuid, task);
        } else {
            int taskId = core.getPlugin().getServer().getScheduler().runTaskLater(core.getPlugin(), () -> {
                if (pendingInput.remove(uuid) != null) {
                    Player p = core.getPlugin().getServer().getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        String msgKey;
                        if (mode == MODE_BUGREPORT) {
                            msgKey = "bugreport-input-timeout";
                        } else if (mode == MODE_SUPPORT) {
                            msgKey = "support-input-timeout";
                        } else {
                            msgKey = "feedback-input-timeout";
                        }
                        p.sendMessage(core.getMessage(msgKey));
                    }
                }
                timeoutTasks.remove(uuid);
            }, timeout * 20L).getTaskId();
            timeoutTasks.put(uuid, taskId);
        }
    }

    private void cancelTimeout(UUID uuid) {
        Object task = timeoutTasks.remove(uuid);
        if (task == null) return;

        if (SchedulerUtil.isFolia()) {
            if (task instanceof io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask) {
                foliaTask.cancel();
            }
        } else {
            if (task instanceof Integer taskId) {
                core.getPlugin().getServer().getScheduler().cancelTask(taskId);
            }
        }
    }

    public boolean isWaitingInput(UUID uuid) {
        return pendingInput.containsKey(uuid);
    }

    public void clearAll() {
        for (UUID uuid : timeoutTasks.keySet()) {
            cancelTimeout(uuid);
        }
        pendingInput.clear();
        timeoutTasks.clear();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Integer mode = pendingInput.get(uuid);
        if (mode == null) return;

        event.setCancelled(true);

        String message = event.getMessage().trim();
        String cancelWord = core.getPlugin().getConfig().getString("chat-input.cancel-word", "cancel");

        // Cancel timeout task
        cancelTimeout(uuid);
        pendingInput.remove(uuid);

        if (message.equalsIgnoreCase(cancelWord)) {
            String cancelKey;
            if (mode == MODE_BUGREPORT) {
                cancelKey = "bugreport-input-cancelled";
            } else if (mode == MODE_SUPPORT) {
                cancelKey = "support-input-cancelled";
            } else {
                cancelKey = "feedback-input-cancelled";
            }
            // Use entity scheduler for Folia compatibility
            SchedulerUtil.runEntityTask(core.getPlugin(), player, () ->
                    player.sendMessage(core.getMessage(cancelKey)));
            return;
        }

        // Create on the entity's owning thread (Folia) or main thread (Spigot/Paper)
        SchedulerUtil.runEntityTask(core.getPlugin(), player, () -> {
            if (mode == MODE_BUGREPORT) {
                BugReport report = core.getBugReportManager().createReport(player, message);
                String successMessage = core.getMessage("bugreport-created")
                        .replace("{id}", report.getId().toString().substring(0, 8));
                player.sendMessage(successMessage);
            } else if (mode == MODE_SUPPORT) {
                SupportTicket ticket = core.getSupportTicketManager().createTicket(player, message);
                String successMessage = core.getMessage("support-created")
                        .replace("{id}", ticket.getId().toString().substring(0, 8));
                player.sendMessage(successMessage);
            } else {
                Feedback feedback = core.getFeedbackManager().createFeedback(player, message);
                String successMessage = core.getMessage("feedback-created")
                        .replace("{id}", feedback.getId().toString().substring(0, 8));
                player.sendMessage(successMessage);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        pendingInput.remove(uuid);
        cancelTimeout(uuid);
    }
}
