package com.turtle.turtlegopy.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.turtle.turtlegopy.core.TurtleGopyCore;
import com.turtle.turtlegopy.gui.AdminBugReportGUI;
import com.turtle.turtlegopy.gui.AdminGUI;
import com.turtle.turtlegopy.gui.PlayerBugReportGUI;
import com.turtle.turtlegopy.gui.PlayerGUI;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class GopyCommand implements CommandExecutor, TabCompleter {

    private final TurtleGopyCore core;

    public GopyCommand(TurtleGopyCore core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean isBaoLoi = label.equalsIgnoreCase("baoloi");

        // No args: /gopy → feedback GUI, /baoloi → bug report GUI
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(core.getMessage("player-only"));
                return true;
            }

            if (isBaoLoi) {
                PlayerBugReportGUI.open(core, player, 0);
            } else {
                PlayerGUI.open(core, player, 0);
            }
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

                // /baoloi check → admin bug report, /gopy check → admin feedback
                if (isBaoLoi) {
                    AdminBugReportGUI.open(core, player, 0, null);
                } else {
                    AdminGUI.open(core, player, 0, null);
                }
            }



            case "checkbaoloi" -> {
                // /gopy checkbaoloi → admin bug report GUI
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

            case "reload" -> {
                if (!sender.hasPermission("turtlegopy.admin")) {
                    sender.sendMessage(core.getMessage("no-permission"));
                    return true;
                }

                core.reload();
                sender.sendMessage(core.getMessage("reload-success"));
            }

            default -> {
                // Quick create: /gopy <content> or /baoloi <content>
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(core.getMessage("player-only"));
                    return true;
                }

                String content = String.join(" ", args);

                if (isBaoLoi) {
                    var report = core.getBugReportManager().createReport(player, content);
                    player.sendMessage(core.getMessage("bugreport-created")
                            .replace("{id}", report.getId().toString().substring(0, 8)));
                } else {
                    var feedback = core.getFeedbackManager().createFeedback(player, content);
                    player.sendMessage(core.getMessage("feedback-created")
                            .replace("{id}", feedback.getId().toString().substring(0, 8)));
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
                completions.add("reload");
            }

            String input = args[0].toLowerCase();
            completions.removeIf(s -> !s.startsWith(input));
            return completions;
        }

        return Collections.emptyList();
    }
}
