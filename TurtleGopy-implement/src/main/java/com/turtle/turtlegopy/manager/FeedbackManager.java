package com.turtle.turtlegopy.manager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.turtle.turtlegopy.api.model.Feedback;
import com.turtle.turtlegopy.api.model.FeedbackStatus;
import com.turtle.turtlegopy.core.SchedulerUtil;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import lombok.Getter;

@Getter
public class FeedbackManager {

    private final TurtleGopyCore core;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public FeedbackManager(TurtleGopyCore core) {
        this.core = core;
    }

    public Feedback createFeedback(Player player, String content) {
        Feedback feedback = Feedback.builder()
                .id(UUID.randomUUID())
                .playerUUID(player.getUniqueId())
                .playerName(player.getName())
                .content(content)
                .status(FeedbackStatus.PENDING)
                .createdAt(System.currentTimeMillis())
                .adminNote("")
                .rewardGiven(false)
                .build();

        core.getStorageProvider().save(feedback);

        notifyAdmins(player, content);

        // Discord notification
        core.getDiscordBotManager().sendFeedbackCreated(
                feedback.getId().toString(),
                feedback.getPlayerName(),
                feedback.getPlayerUUID().toString(),
                feedback.getContent(),
                feedback.getStatus().name(),
                feedback.getCreatedAt()
        );

        return feedback;
    }

    public void updateStatus(UUID feedbackId, FeedbackStatus newStatus) {
        Feedback feedback = core.getStorageProvider().getById(feedbackId);
        if (feedback == null) return;

        feedback.setStatus(newStatus);
        core.getStorageProvider().update(feedback);

        if (core.getPlugin().getConfig().getBoolean("notification.on-status-change", true)) {
            Player player = Bukkit.getPlayer(feedback.getPlayerUUID());
            if (player != null && player.isOnline()) {
                String message = core.getMessage("feedback-status-changed")
                        .replace("{status}", newStatus.getDisplayName());
                player.sendMessage(message);
            }
        }

        // Discord status update
        core.getDiscordBotManager().sendFeedbackStatusUpdate(feedbackId.toString(), newStatus.name());

        if (newStatus == FeedbackStatus.ACCEPTED && !feedback.isRewardGiven()) {
            giveReward(feedback);
        }
    }

    public void setAdminNote(UUID feedbackId, String note) {
        Feedback feedback = core.getStorageProvider().getById(feedbackId);
        if (feedback == null) return;

        feedback.setAdminNote(note);
        core.getStorageProvider().update(feedback);
    }

    public void deleteFeedback(UUID feedbackId) {
        core.getStorageProvider().delete(feedbackId);
    }

    public Feedback getFeedback(UUID feedbackId) {
        return core.getStorageProvider().getById(feedbackId);
    }

    public List<Feedback> getPlayerFeedbacks(UUID playerUUID) {
        return core.getStorageProvider().getByPlayer(playerUUID);
    }

    public List<Feedback> getAllFeedbacks() {
        return core.getStorageProvider().getAll();
    }

    public String formatDate(long timestamp) {
        return dateFormat.format(new Date(timestamp));
    }

    private void giveReward(Feedback feedback) {
        if (!core.getPlugin().getConfig().getBoolean("rewards.enabled", true)) return;

        List<String> commands = core.getPlugin().getConfig().getStringList("rewards.commands");
        if (commands.isEmpty()) return;

        Player player = Bukkit.getPlayer(feedback.getPlayerUUID());
        if (player != null && player.isOnline()) {
            // Use global scheduler for command dispatch (Folia-compatible)
            SchedulerUtil.runGlobalTask(core.getPlugin(), () -> {
                for (String cmd : commands) {
                    String parsed = cmd.replace("{player}", player.getName())
                            .replace("{feedback_id}", feedback.getId().toString());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                }
            });

            feedback.setRewardGiven(true);
            core.getStorageProvider().update(feedback);

            player.sendMessage(core.getMessage("feedback-reward"));
        } else {
            feedback.setRewardGiven(false);
            core.getStorageProvider().update(feedback);
        }
    }

    private void notifyAdmins(Player sender, String content) {
        if (!core.getPlugin().getConfig().getBoolean("notification.on-new-feedback", true)) return;

        String message = core.getMessage("admin-new-feedback")
                .replace("{player}", sender.getName())
                .replace("{content}", content.length() > 50 ? content.substring(0, 50) + "..." : content);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("turtlegopy.admin")) {
                online.sendMessage(message);
            }
        }
    }
}
