package com.turtle.turtlegopy.manager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.turtle.turtlegopy.api.model.BugReport;
import com.turtle.turtlegopy.api.model.BugReportStatus;
import com.turtle.turtlegopy.core.SchedulerUtil;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import lombok.Getter;

@Getter
public class BugReportManager {

    private final TurtleGopyCore core;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public BugReportManager(TurtleGopyCore core) {
        this.core = core;
    }

    public BugReport createReport(Player player, String content) {
        BugReport report = BugReport.builder()
                .id(UUID.randomUUID())
                .playerUUID(player.getUniqueId())
                .playerName(player.getName())
                .content(content)
                .status(BugReportStatus.PENDING)
                .createdAt(System.currentTimeMillis())
                .adminNote("")
                .rewardGiven(false)
                .build();

        core.getBugReportStorageProvider().save(report);

        notifyAdmins(player, content);

        // Discord notification
        core.getDiscordBotManager().sendBugReportCreated(
                report.getId().toString(),
                report.getPlayerName(),
                report.getPlayerUUID().toString(),
                report.getContent(),
                report.getStatus().name(),
                report.getCreatedAt()
        );

        return report;
    }

    public void updateStatus(UUID reportId, BugReportStatus newStatus) {
        BugReport report = core.getBugReportStorageProvider().getById(reportId);
        if (report == null) return;

        report.setStatus(newStatus);
        core.getBugReportStorageProvider().update(report);

        if (core.getPlugin().getConfig().getBoolean("notification.on-status-change", true)) {
            Player player = Bukkit.getPlayer(report.getPlayerUUID());
            if (player != null && player.isOnline()) {
                String message = core.getMessage("bugreport-status-changed")
                        .replace("{status}", newStatus.getDisplayName());
                player.sendMessage(message);
            }
        }

        // Discord status update
        core.getDiscordBotManager().sendBugReportStatusUpdate(reportId.toString(), newStatus.name());

        if (newStatus == BugReportStatus.FIXED && !report.isRewardGiven()) {
            giveReward(report);
        }
    }

    public void setAdminNote(UUID reportId, String note) {
        BugReport report = core.getBugReportStorageProvider().getById(reportId);
        if (report == null) return;

        report.setAdminNote(note);
        core.getBugReportStorageProvider().update(report);
    }

    public void deleteReport(UUID reportId) {
        core.getBugReportStorageProvider().delete(reportId);
    }

    public BugReport getReport(UUID reportId) {
        return core.getBugReportStorageProvider().getById(reportId);
    }

    public List<BugReport> getPlayerReports(UUID playerUUID) {
        return core.getBugReportStorageProvider().getByPlayer(playerUUID);
    }

    public List<BugReport> getAllReports() {
        return core.getBugReportStorageProvider().getAll();
    }

    public String formatDate(long timestamp) {
        return dateFormat.format(new Date(timestamp));
    }

    public List<BugReport> getPendingRewards(UUID playerUUID) {
        List<BugReport> all = core.getBugReportStorageProvider().getByPlayer(playerUUID);
        List<BugReport> pending = new java.util.ArrayList<>();
        for (BugReport r : all) {
            if (r.isRewardPending() && !r.isRewardGiven()) {
                pending.add(r);
            }
        }
        return pending;
    }

    public void claimReward(UUID reportId, String option) {
        BugReport report = core.getBugReportStorageProvider().getById(reportId);
        if (report == null || !report.isRewardPending() || report.isRewardGiven()) return;

        if (!core.getPlugin().getConfig().getBoolean("bugreport-rewards.enabled", true)) return;

        List<String> commands = core.getPlugin().getConfig().getStringList("bugreport-rewards." + option + ".commands");
        if (commands.isEmpty()) return;

        Player player = Bukkit.getPlayer(report.getPlayerUUID());
        if (player != null && player.isOnline()) {
            SchedulerUtil.runGlobalTask(core.getPlugin(), () -> {
                for (String cmd : commands) {
                    String parsed = cmd.replace("{player}", player.getName())
                            .replace("{report_id}", report.getId().toString());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                }
            });

            report.setRewardGiven(true);
            report.setRewardPending(false);
            core.getBugReportStorageProvider().update(report);

            player.sendMessage(core.getMessage("reward-claimed"));
        }
    }

    private void giveReward(BugReport report) {
        if (!core.getPlugin().getConfig().getBoolean("bugreport-rewards.enabled", true)) return;

        // Set reward as pending - player must claim via /nhanthuong
        report.setRewardPending(true);
        report.setRewardGiven(false);
        core.getBugReportStorageProvider().update(report);

        Player player = Bukkit.getPlayer(report.getPlayerUUID());
        if (player != null && player.isOnline()) {
            player.sendMessage(core.getMessage("reward-pending"));
        }
    }

    private void notifyAdmins(Player sender, String content) {
        if (!core.getPlugin().getConfig().getBoolean("notification.on-new-bugreport", true)) return;

        String message = core.getMessage("admin-new-bugreport")
                .replace("{player}", sender.getName())
                .replace("{content}", content.length() > 50 ? content.substring(0, 50) + "..." : content);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("turtlegopy.admin")) {
                online.sendMessage(message);
            }
        }
    }
}
