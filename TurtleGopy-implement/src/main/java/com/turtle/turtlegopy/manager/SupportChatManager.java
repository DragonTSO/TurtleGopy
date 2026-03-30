package com.turtle.turtlegopy.manager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.turtle.turtlegopy.api.model.SupportChatMessage;
import com.turtle.turtlegopy.api.model.SupportTicket;
import com.turtle.turtlegopy.core.SchedulerUtil;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import lombok.Getter;

@Getter
public class SupportChatManager {

    private final TurtleGopyCore core;
    private final Map<UUID, UUID> activeSessions = new ConcurrentHashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM HH:mm");

    public SupportChatManager(TurtleGopyCore core) {
        this.core = core;
    }

    /**
     * Player/Admin enters a support chat session for a ticket.
     * Shows full chat history.
     */
    public void enterChat(Player player, UUID ticketId) {
        SupportTicket ticket = core.getSupportTicketManager().getTicket(ticketId);
        if (ticket == null) return;

        activeSessions.put(player.getUniqueId(), ticketId);

        // Show chat history header
        player.sendMessage(core.getMessageNoPrefix("support-chat-history-header"));

        // Load and show ALL chat history
        List<SupportChatMessage> messages = core.getSupportChatStorageProvider().getMessages(ticketId);

        if (messages.isEmpty()) {
            player.sendMessage(core.getMessageNoPrefix("support-chat-no-history"));
        } else {
            for (SupportChatMessage msg : messages) {
                String roleTag = msg.isStaff()
                        ? core.getMessageNoPrefix("support-chat-role-staff")
                        : core.getMessageNoPrefix("support-chat-role-player");
                String timeStr = "§8[" + dateFormat.format(new Date(msg.getTimestamp())) + "]";
                String formatted = core.getMessageNoPrefix("support-chat-format")
                        .replace("{role}", roleTag)
                        .replace("{player}", msg.getSenderName())
                        .replace("{message}", msg.getMessage())
                        .replace("{time}", timeStr);
                player.sendMessage(formatted);
            }
        }

        // Show footer with instructions
        player.sendMessage(core.getMessageNoPrefix("support-chat-history-footer"));
        player.sendMessage(core.getMessage("support-chat-enter"));
    }

    /**
     * Player/Admin leaves the support chat session.
     */
    public void leaveChat(Player player) {
        UUID removed = activeSessions.remove(player.getUniqueId());
        if (removed != null) {
            player.sendMessage(core.getMessage("support-chat-exit"));
        }
    }

    /**
     * Check if a player is currently in a support chat session.
     */
    public boolean isInChat(UUID playerUUID) {
        return activeSessions.containsKey(playerUUID);
    }

    /**
     * Get the ticketId for a player's active chat session.
     */
    public UUID getTicketId(UUID playerUUID) {
        return activeSessions.get(playerUUID);
    }

    /**
     * Send a message in the support chat. Persists to storage and
     * delivers to all participants and admins.
     */
    public void sendMessage(Player sender, String message) {
        UUID ticketId = activeSessions.get(sender.getUniqueId());
        if (ticketId == null) return;

        SupportTicket ticket = core.getSupportTicketManager().getTicket(ticketId);
        if (ticket == null) return;

        boolean isStaff = sender.hasPermission("turtlegopy.admin");

        // Create and save message
        SupportChatMessage chatMsg = SupportChatMessage.builder()
                .id(UUID.randomUUID())
                .ticketId(ticketId)
                .senderUUID(sender.getUniqueId())
                .senderName(sender.getName())
                .message(message)
                .timestamp(System.currentTimeMillis())
                .staff(isStaff)
                .build();

        // Save asynchronously
        SchedulerUtil.runAsync(core.getPlugin(), () ->
                core.getSupportChatStorageProvider().saveMessage(chatMsg));

        // Format the message
        String roleTag = isStaff
                ? core.getMessageNoPrefix("support-chat-role-staff")
                : core.getMessageNoPrefix("support-chat-role-player");
        String formatted = core.getMessageNoPrefix("support-chat-format")
                .replace("{role}", roleTag)
                .replace("{player}", sender.getName())
                .replace("{message}", message)
                .replace("{time}", "");

        String ticketShortId = ticketId.toString().substring(0, 8);

        // Send to all players in the same chat session
        for (Map.Entry<UUID, UUID> entry : activeSessions.entrySet()) {
            if (entry.getValue().equals(ticketId)) {
                Player participant = Bukkit.getPlayer(entry.getKey());
                if (participant != null && participant.isOnline()
                        && !participant.getUniqueId().equals(sender.getUniqueId())) {
                    participant.sendMessage(formatted);
                }
            }
        }

        // Send to sender themselves
        sender.sendMessage(formatted);

        // Notify admins who are NOT in the chat session but are online
        String notification = core.getMessage("support-chat-notify-new")
                .replace("{player}", sender.getName())
                .replace("{id}", ticketShortId);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("turtlegopy.admin")
                    && !activeSessions.containsKey(online.getUniqueId())
                    && !online.getUniqueId().equals(sender.getUniqueId())) {
                online.sendMessage(notification);
            }
        }

        // Also notify the ticket owner if they're online but not in chat
        Player ticketOwner = Bukkit.getPlayer(ticket.getPlayerUUID());
        if (ticketOwner != null && ticketOwner.isOnline()
                && !activeSessions.containsKey(ticketOwner.getUniqueId())
                && !ticketOwner.getUniqueId().equals(sender.getUniqueId())) {
            String ownerNotification = core.getMessage("support-chat-notify-new")
                    .replace("{player}", sender.getName())
                    .replace("{id}", ticketShortId);
            ticketOwner.sendMessage(ownerNotification);
        }
    }

    /**
     * Remove a player from chat (e.g., on quit).
     */
    public void removePlayer(UUID playerUUID) {
        activeSessions.remove(playerUUID);
    }

    /**
     * Clear all sessions (on plugin disable).
     */
    public void clearAll() {
        activeSessions.clear();
    }
}
