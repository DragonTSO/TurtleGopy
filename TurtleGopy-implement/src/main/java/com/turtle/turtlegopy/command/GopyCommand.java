package com.turtle.turtlegopy.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.turtle.turtlegopy.core.TurtleGopyCore;
import com.turtle.turtlegopy.gui.AdminBugReportGUI;
import com.turtle.turtlegopy.gui.AdminGUI;
import com.turtle.turtlegopy.gui.AdminSupportGUI;
import com.turtle.turtlegopy.gui.PlayerBugReportGUI;
import com.turtle.turtlegopy.gui.PlayerGUI;
import com.turtle.turtlegopy.gui.PlayerSupportGUI;

import org.bukkit.Bukkit;
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
        boolean isHoTro = label.equalsIgnoreCase("hotro");

        // No args: /gopy → feedback GUI, /baoloi → bug report GUI, /hotro → support GUI
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(core.getMessage("player-only"));
                return true;
            }

            if (isBaoLoi) {
                PlayerBugReportGUI.open(core, player, 0);
            } else if (isHoTro) {
                PlayerSupportGUI.open(core, player, 0);
            } else {
                PlayerGUI.open(core, player, 0);
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "exit" -> {
                // /hotro exit → leave support chat
                if (!isHoTro) break;
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(core.getMessage("player-only"));
                    return true;
                }

                if (core.getSupportChatManager().isInChat(player.getUniqueId())) {
                    core.getSupportChatManager().leaveChat(player);
                } else {
                    player.sendMessage(core.getMessage("support-chat-not-in-chat"));
                }
            }

            case "check" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(core.getMessage("player-only"));
                    return true;
                }

                if (!player.hasPermission("turtlegopy.admin")) {
                    player.sendMessage(core.getMessage("no-permission"));
                    return true;
                }

                String playerFilter = args.length >= 2 ? args[1] : null;

                // /baoloi check [player] → admin bug report, /hotro check [player] → admin support, /gopy check [player] → admin feedback
                if (isBaoLoi) {
                    AdminBugReportGUI.open(core, player, 0, null, playerFilter);
                } else if (isHoTro) {
                    AdminSupportGUI.open(core, player, 0, null, playerFilter);
                } else {
                    AdminGUI.open(core, player, 0, null, playerFilter);
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

            case "checkhotro" -> {
                // /gopy checkhotro → admin support GUI
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(core.getMessage("player-only"));
                    return true;
                }

                if (!player.hasPermission("turtlegopy.admin")) {
                    player.sendMessage(core.getMessage("no-permission"));
                    return true;
                }

                AdminSupportGUI.open(core, player, 0, null);
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
                // Quick create: /gopy <content>, /baoloi <content>, /hotro <content>
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(core.getMessage("player-only"));
                    return true;
                }

                String content = String.join(" ", args);

                if (isBaoLoi) {
                    var report = core.getBugReportManager().createReport(player, content);
                    player.sendMessage(core.getMessage("bugreport-created")
                            .replace("{id}", report.getId().toString().substring(0, 8)));
                } else if (isHoTro) {
                    if (core.getSupportTicketManager().hasActiveTicket(player.getUniqueId())) {
                        player.sendMessage(core.getMessage("support-already-active"));
                        return true;
                    }
                    var ticket = core.getSupportTicketManager().createTicket(player, content);
                    player.sendMessage(core.getMessage("support-created")
                            .replace("{id}", ticket.getId().toString().substring(0, 8)));
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
            if (label.equalsIgnoreCase("hotro")) {
                completions.add("exit");
            }

            String input = args[0].toLowerCase();
            completions.removeIf(s -> !s.startsWith(input));
            return completions;
        }

        // Tab complete player names for /check <player>
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
