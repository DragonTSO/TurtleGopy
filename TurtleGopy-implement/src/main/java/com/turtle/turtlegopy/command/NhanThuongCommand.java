package com.turtle.turtlegopy.command;

import java.util.Collections;
import java.util.List;

import com.turtle.turtlegopy.core.TurtleGopyCore;
import com.turtle.turtlegopy.gui.RewardListGUI;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class NhanThuongCommand implements CommandExecutor, TabCompleter {

    private final TurtleGopyCore core;

    public NhanThuongCommand(TurtleGopyCore core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(core.getMessage("player-only"));
            return true;
        }

        // Check if player has any pending rewards
        int feedbackCount = core.getFeedbackManager().getPendingRewards(player.getUniqueId()).size();
        int bugReportCount = core.getBugReportManager().getPendingRewards(player.getUniqueId()).size();

        if (feedbackCount + bugReportCount == 0) {
            player.sendMessage(core.getMessage("reward-no-pending"));
            return true;
        }

        RewardListGUI.open(core, player, 0);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
