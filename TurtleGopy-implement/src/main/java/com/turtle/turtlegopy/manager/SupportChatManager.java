package com.turtle.turtlegopy.manager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    // Pending notifications: playerUUID -> set of entityIds with unread staff replies
    private final Map<UUID, Set<UUID>> pendingNotifications = new ConcurrentHashMap<>();

    public SupportChatManager(TurtleGopyCore core) {
        this.core = core;
    }

    /**
     * Start the repeating notification task (call from onEnable).
     * Runs every 3 minutes (3600 ticks) to remind players of unread staff replies.
     */
    public void startNotificationTask() {
        long intervalTicks = 3 * 60 * 20L; // 3 minutes = 3600 ticks
        SchedulerUtil.runGlobalTaskTimer(core.getPlugin(), () -> {
            // Remind about unread staff replies
            for (Map.Entry<UUID, Set<UUID>> entry : pendingNotifications.entrySet()) {
                UUID playerUUID = entry.getKey();
                Set<UUID> entityIds = entry.getValue();
                if (entityIds.isEmpty()) continue;

                UUID activeSession = activeSessions.get(playerUUID);

                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null && player.isOnline()) {
                    for (UUID entityId : entityIds) {
                        if (entityId.equals(activeSession)) continue;

                        String shortId = entityId.toString().substring(0, 8);
                        String entityType = resolveEntityType(entityId);
                        String reminder = core.getMessage("staff-reply-reminder")
                                .replace("{id}", shortId)
                                .replace("{type}", entityType);
                        player.sendMessage(reminder);
                    }
                }
            }

            // Remind about unclaimed rewards
            for (Player online : Bukkit.getOnlinePlayers()) {
                UUID uuid = online.getUniqueId();
                int feedbackRewards = core.getFeedbackManager().getPendingRewards(uuid).size();
                int bugRewards = core.getBugReportManager().getPendingRewards(uuid).size();
                int total = feedbackRewards + bugRewards;
                if (total > 0) {
                    String rewardReminder = core.getMessage("reward-pending-reminder")
                            .replace("{count}", String.valueOf(total));
                    online.sendMessage(rewardReminder);
                }
            }
        }, intervalTicks, intervalTicks);
    }

    /**
     * Player/Admin enters a support chat session for a ticket.
     * Shows full chat history.
     */
    public void enterChat(Player player, UUID ticketId) {
        SupportTicket ticket = core.getSupportTicketManager().getTicket(ticketId);
        if (ticket == null) {
            // Try as a generic chat (feedback/bugreport)
            enterGenericChat(player, ticketId);
            return;
        }

        activeSessions.put(player.getUniqueId(), ticketId);
        clearNotification(player.getUniqueId(), ticketId);
        showChatHistory(player, ticketId);
    }

    /**
     * Enter chat for feedback or bug report (uses same storage as support chat).
     */
    public void enterGenericChat(Player player, UUID entityId) {
        activeSessions.put(player.getUniqueId(), entityId);
        clearNotification(player.getUniqueId(), entityId);
        showChatHistory(player, entityId);
    }

    private void showChatHistory(Player player, UUID entityId) {
        // Show chat history header
        player.sendMessage(core.getMessageNoPrefix("support-chat-history-header"));

        // Load and show ALL chat history
        List<SupportChatMessage> messages = core.getSupportChatStorageProvider().getMessages(entityId);

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
     * Send a message in the chat. Persists to storage and
     * delivers to all participants and admins.
     * Works for support tickets, feedbacks, and bug reports.
     */
    public void sendMessage(Player sender, String message) {
        UUID entityId = activeSessions.get(sender.getUniqueId());
        if (entityId == null) return;

        // Resolve entity owner UUID
        UUID ownerUUID = resolveEntityOwner(entityId);

        boolean isStaff = sender.hasPermission("turtlegopy.admin");

        // Create and save message
        SupportChatMessage chatMsg = SupportChatMessage.builder()
                .id(UUID.randomUUID())
                .ticketId(entityId)
                .senderUUID(sender.getUniqueId())
                .senderName(sender.getName())
                .message(message)
                .timestamp(System.currentTimeMillis())
                .staff(isStaff)
                .build();

        // Save asynchronously
        SchedulerUtil.runAsync(core.getPlugin(), () ->
                core.getSupportChatStorageProvider().saveMessage(chatMsg));

        // Forward to Discord
        core.getDiscordBotManager().sendSupportChatMessage(
                entityId.toString(), sender.getName(), message, isStaff);

        // Format the message
        String roleTag = isStaff
                ? core.getMessageNoPrefix("support-chat-role-staff")
                : core.getMessageNoPrefix("support-chat-role-player");
        String formatted = core.getMessageNoPrefix("support-chat-format")
                .replace("{role}", roleTag)
                .replace("{player}", sender.getName())
                .replace("{message}", message)
                .replace("{time}", "");

        String shortId = entityId.toString().substring(0, 8);

        // Send to all players in the same chat session
        for (Map.Entry<UUID, UUID> entry : activeSessions.entrySet()) {
            if (entry.getValue().equals(entityId)) {
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
                .replace("{id}", shortId);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("turtlegopy.admin")
                    && !activeSessions.containsKey(online.getUniqueId())
                    && !online.getUniqueId().equals(sender.getUniqueId())) {
                online.sendMessage(notification);
            }
        }

        // Also notify the entity owner if they're online but not in chat
        if (ownerUUID != null) {
            Player entityOwner = Bukkit.getPlayer(ownerUUID);
            if (entityOwner != null && entityOwner.isOnline()
                    && !activeSessions.containsKey(entityOwner.getUniqueId())
                    && !entityOwner.getUniqueId().equals(sender.getUniqueId())) {
                entityOwner.sendMessage(notification);
            }
        }

        // If staff replied and owner is NOT in this chat session, add to pending notifications
        if (isStaff && ownerUUID != null) {
            UUID ownerActiveSession = activeSessions.get(ownerUUID);
            if (!entityId.equals(ownerActiveSession)) {
                addNotification(ownerUUID, entityId);
            }
        }
    }

    /**
     * Add a pending notification for a player about an entity.
     */
    private void addNotification(UUID playerUUID, UUID entityId) {
        pendingNotifications.computeIfAbsent(playerUUID, k -> ConcurrentHashMap.newKeySet()).add(entityId);
    }

    /**
     * Clear a specific notification when player enters that chat.
     */
    public void clearNotification(UUID playerUUID, UUID entityId) {
        Set<UUID> entities = pendingNotifications.get(playerUUID);
        if (entities != null) {
            entities.remove(entityId);
            if (entities.isEmpty()) {
                pendingNotifications.remove(playerUUID);
            }
        }
    }

    /**
     * Check and notify player on join about any unread staff replies.
     */
    public void onPlayerJoin(UUID playerUUID) {
        Set<UUID> entities = pendingNotifications.get(playerUUID);
        if (entities == null || entities.isEmpty()) return;

        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) return;

        // Delay notification slightly so player sees it after join messages
        SchedulerUtil.runEntityTaskLater(core.getPlugin(), player, () -> {
            // Notify about unread staff replies
            Set<UUID> current = pendingNotifications.get(playerUUID);
            if (current != null && !current.isEmpty()) {
                for (UUID entityId : current) {
                    String shortId = entityId.toString().substring(0, 8);
                    String entityType = resolveEntityType(entityId);
                    String reminder = core.getMessage("staff-reply-reminder")
                            .replace("{id}", shortId)
                            .replace("{type}", entityType);
                    player.sendMessage(reminder);
                }
            }

            // Notify about unclaimed rewards
            int feedbackRewards = core.getFeedbackManager().getPendingRewards(playerUUID).size();
            int bugRewards = core.getBugReportManager().getPendingRewards(playerUUID).size();
            int total = feedbackRewards + bugRewards;
            if (total > 0) {
                String rewardReminder = core.getMessage("reward-pending-reminder")
                        .replace("{count}", String.valueOf(total));
                player.sendMessage(rewardReminder);
            }
        }, 60L); // 3 seconds delay
    }

    /**
     * Resolve the owner UUID of a ticket/feedback/bugreport by its ID.
     */
    private UUID resolveEntityOwner(UUID entityId) {
        com.turtle.turtlegopy.api.model.SupportTicket ticket = core.getSupportTicketManager().getTicket(entityId);
        if (ticket != null) return ticket.getPlayerUUID();

        com.turtle.turtlegopy.api.model.Feedback feedback = core.getFeedbackManager().getFeedback(entityId);
        if (feedback != null) return feedback.getPlayerUUID();

        com.turtle.turtlegopy.api.model.BugReport report = core.getBugReportManager().getReport(entityId);
        if (report != null) return report.getPlayerUUID();

        return null;
    }

    /**
     * Resolve the type label of an entity.
     */
    private String resolveEntityType(UUID entityId) {
        com.turtle.turtlegopy.api.model.SupportTicket ticket = core.getSupportTicketManager().getTicket(entityId);
        if (ticket != null) return "Hỗ Trợ";

        com.turtle.turtlegopy.api.model.Feedback feedback = core.getFeedbackManager().getFeedback(entityId);
        if (feedback != null) return "Góp Ý";

        com.turtle.turtlegopy.api.model.BugReport report = core.getBugReportManager().getReport(entityId);
        if (report != null) return "Báo Lỗi";

        return "Phiếu";
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
        pendingNotifications.clear();
    }
}
