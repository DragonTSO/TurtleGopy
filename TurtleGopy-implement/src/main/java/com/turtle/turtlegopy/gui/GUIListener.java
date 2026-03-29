package com.turtle.turtlegopy.gui;

import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GUIListener implements Listener {

    private final TurtleGopyCore core;

    public GUIListener(TurtleGopyCore core) {
        this.core = core;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Feedback GUIs
        if (event.getInventory().getHolder() instanceof PlayerGUI) {
            PlayerGUI.handleClick(core, event);
            return;
        }

        if (event.getInventory().getHolder() instanceof AdminGUI) {
            AdminGUI.handleClick(core, event);
            return;
        }

        if (event.getInventory().getHolder() instanceof AdminManageGUI) {
            AdminManageGUI.handleClick(core, event);
            return;
        }

        // Bug Report GUIs
        if (event.getInventory().getHolder() instanceof PlayerBugReportGUI) {
            PlayerBugReportGUI.handleClick(core, event);
            return;
        }

        if (event.getInventory().getHolder() instanceof AdminBugReportGUI) {
            AdminBugReportGUI.handleClick(core, event);
            return;
        }

        if (event.getInventory().getHolder() instanceof AdminBugManageGUI) {
            AdminBugManageGUI.handleClick(core, event);
            return;
        }

        // Support Ticket GUIs
        if (event.getInventory().getHolder() instanceof PlayerSupportGUI) {
            PlayerSupportGUI.handleClick(core, event);
            return;
        }

        if (event.getInventory().getHolder() instanceof AdminSupportGUI) {
            AdminSupportGUI.handleClick(core, event);
            return;
        }

        if (event.getInventory().getHolder() instanceof AdminSupportManageGUI) {
            AdminSupportManageGUI.handleClick(core, event);
        }
    }
}
