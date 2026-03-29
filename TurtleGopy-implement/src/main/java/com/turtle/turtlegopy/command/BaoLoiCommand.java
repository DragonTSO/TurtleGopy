package com.turtle.turtlegopy.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.turtle.turtlegopy.core.TurtleGopyCore;
import com.turtle.turtlegopy.gui.AdminBugReportGUI;
import com.turtle.turtlegopy.gui.PlayerBugReportGUI;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class BaoLoiCommand implements CommandExecutor, TabCompleter {

    private final TurtleGopyCore core;

    public BaoLoiCommand(TurtleGopyCore core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(core.getMessage("player-only"));
                return true;
            }

            if (!player.hasPermission("turtlegopy.bugreport.use")) {
                player.sendMessage(core.getMessage("no-permission"));
                return true;
            }

            PlayerBugReportGUI.open(core, player, 0);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "check" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(core.getMessage("player-only"));
                    return true;
                }

                if (!player.hasPermission("turtlegopy.admin")) {
                    player.sendMessage(core.getMessage("no-permission"));
                    return true;
                }

                AdminBugReportGUI.open(core, player, 0, null);
            }

            default -> {
                sender.sendMessage(core.colorize("&c&lBáo Lỗi &8- &7Các lệnh:"));
                sender.sendMessage(core.colorize("&e/baoloi &8- &7Mở menu báo lỗi"));
                if (sender.hasPermission("turtlegopy.admin")) {
                    sender.sendMessage(core.colorize("&e/baoloi check &8- &7Quản lý báo lỗi"));
                }
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("turtlegopy.admin")) {
                completions.add("check");
            }

            String input = args[0].toLowerCase();
            completions.removeIf(s -> !s.startsWith(input));
            return completions;
        }

        return Collections.emptyList();
    }
}
