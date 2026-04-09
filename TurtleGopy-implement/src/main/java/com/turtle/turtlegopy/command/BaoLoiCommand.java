package com.turtle.turtlegopy.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.turtle.turtlegopy.core.TurtleGopyCore;
import com.turtle.turtlegopy.gui.AdminBugReportGUI;
import com.turtle.turtlegopy.gui.PlayerBugReportGUI;

import org.bukkit.Bukkit;
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

                AdminBugReportGUI.open(core, player, 0, null, args.length >= 2 ? args[1] : null);
            }

            default -> {
                // Quick create: /baoloi <content>
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(core.getMessage("player-only"));
                    return true;
                }

                String content = String.join(" ", args);
                var report = core.getBugReportManager().createReport(player, content);
                player.sendMessage(core.getMessage("bugreport-created")
                        .replace("{id}", report.getId().toString().substring(0, 8)));
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

        if (args.length == 2 && args[0].equalsIgnoreCase("check") && sender.hasPermission("turtlegopy.admin")) {
            String input = args[1].toLowerCase();
            List<String> players = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(input)) {
                    players.add(online.getName());
                }
            }
            return players;
        }

        return Collections.emptyList();
    }
}
