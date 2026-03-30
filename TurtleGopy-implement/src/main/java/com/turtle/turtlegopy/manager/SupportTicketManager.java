package com.turtle.turtlegopy.manager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.turtle.turtlegopy.api.model.SupportTicket;
import com.turtle.turtlegopy.api.model.SupportTicketStatus;
import com.turtle.turtlegopy.core.SchedulerUtil;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import lombok.Getter;

@Getter
public class SupportTicketManager {

    private final TurtleGopyCore core;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public SupportTicketManager(TurtleGopyCore core) {
        this.core = core;
    }

    /**
     * Check if a player already has an active (not resolved/rejected) ticket.
     */
    public boolean hasActiveTicket(UUID playerUUID) {
        List<SupportTicket> tickets = getPlayerTickets(playerUUID);
        return tickets.stream().anyMatch(t ->
                t.getStatus() != SupportTicketStatus.RESOLVED &&
                t.getStatus() != SupportTicketStatus.REJECTED);
    }

    public SupportTicket createTicket(Player player, String content) {
        SupportTicket ticket = SupportTicket.builder()
                .id(UUID.randomUUID())
                .playerUUID(player.getUniqueId())
                .playerName(player.getName())
                .content(content)
                .status(SupportTicketStatus.PENDING)
                .createdAt(System.currentTimeMillis())
                .adminNote("")
                .rewardGiven(false)
                .build();

        core.getSupportTicketStorageProvider().save(ticket);

        notifyAdmins(player, content);

        return ticket;
    }

    public void updateStatus(UUID ticketId, SupportTicketStatus newStatus) {
        SupportTicket ticket = core.getSupportTicketStorageProvider().getById(ticketId);
        if (ticket == null) return;

        ticket.setStatus(newStatus);
        core.getSupportTicketStorageProvider().update(ticket);

        if (core.getPlugin().getConfig().getBoolean("notification.on-status-change", true)) {
            Player player = Bukkit.getPlayer(ticket.getPlayerUUID());
            if (player != null && player.isOnline()) {
                String message = core.getMessage("support-status-changed")
                        .replace("{status}", newStatus.getDisplayName());
                player.sendMessage(message);
            }
        }

        if (newStatus == SupportTicketStatus.RESOLVED && !ticket.isRewardGiven()) {
            giveReward(ticket);
        }
    }

    public void setAdminNote(UUID ticketId, String note) {
        SupportTicket ticket = core.getSupportTicketStorageProvider().getById(ticketId);
        if (ticket == null) return;

        ticket.setAdminNote(note);
        core.getSupportTicketStorageProvider().update(ticket);
    }

    public void deleteTicket(UUID ticketId) {
        core.getSupportTicketStorageProvider().delete(ticketId);
        // Also delete associated chat messages
        core.getSupportChatStorageProvider().deleteByTicket(ticketId);
    }

    public SupportTicket getTicket(UUID ticketId) {
        return core.getSupportTicketStorageProvider().getById(ticketId);
    }

    public List<SupportTicket> getPlayerTickets(UUID playerUUID) {
        return core.getSupportTicketStorageProvider().getByPlayer(playerUUID);
    }

    public List<SupportTicket> getAllTickets() {
        return core.getSupportTicketStorageProvider().getAll();
    }

    public String formatDate(long timestamp) {
        return dateFormat.format(new Date(timestamp));
    }

    private void giveReward(SupportTicket ticket) {
        if (!core.getPlugin().getConfig().getBoolean("support-rewards.enabled", false)) return;

        List<String> commands = core.getPlugin().getConfig().getStringList("support-rewards.commands");
        if (commands.isEmpty()) return;

        Player player = Bukkit.getPlayer(ticket.getPlayerUUID());
        if (player != null && player.isOnline()) {
            // Use global scheduler for command dispatch (Folia-compatible)
            SchedulerUtil.runGlobalTask(core.getPlugin(), () -> {
                for (String cmd : commands) {
                    String parsed = cmd.replace("{player}", player.getName())
                            .replace("{ticket_id}", ticket.getId().toString());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                }
            });

            ticket.setRewardGiven(true);
            core.getSupportTicketStorageProvider().update(ticket);

            player.sendMessage(core.getMessage("support-reward"));
        } else {
            ticket.setRewardGiven(false);
            core.getSupportTicketStorageProvider().update(ticket);
        }
    }

    private void notifyAdmins(Player sender, String content) {
        if (!core.getPlugin().getConfig().getBoolean("notification.on-new-support", true)) return;

        String message = core.getMessage("admin-new-support")
                .replace("{player}", sender.getName())
                .replace("{content}", content.length() > 50 ? content.substring(0, 50) + "..." : content);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("turtlegopy.admin")) {
                online.sendMessage(message);
            }
        }
    }
}
