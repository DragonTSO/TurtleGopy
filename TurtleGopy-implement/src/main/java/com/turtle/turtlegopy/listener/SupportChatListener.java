package com.turtle.turtlegopy.listener;

import java.util.UUID;

import com.turtle.turtlegopy.core.SchedulerUtil;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SupportChatListener implements Listener {

    private final TurtleGopyCore core;

    public SupportChatListener(TurtleGopyCore core) {
        this.core = core;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!core.getSupportChatManager().isInChat(uuid)) return;

        // Cancel the event so global chat doesn't see it
        event.setCancelled(true);

        String message = event.getMessage().trim();
        String exitWord = core.getPlugin().getConfig().getString("support-chat.exit-word", "exit");

        if (message.equalsIgnoreCase(exitWord)) {
            // Leave chat - must run on entity thread for Folia compatibility
            SchedulerUtil.runEntityTask(core.getPlugin(), player, () ->
                    core.getSupportChatManager().leaveChat(player));
            return;
        }

        // Send message in support chat - run on entity thread
        SchedulerUtil.runEntityTask(core.getPlugin(), player, () ->
                core.getSupportChatManager().sendMessage(player, message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        core.getSupportChatManager().removePlayer(event.getPlayer().getUniqueId());
    }
}
