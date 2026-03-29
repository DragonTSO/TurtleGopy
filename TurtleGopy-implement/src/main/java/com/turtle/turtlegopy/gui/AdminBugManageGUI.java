package com.turtle.turtlegopy.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.turtle.turtlegopy.api.model.BugReport;
import com.turtle.turtlegopy.api.model.BugReportStatus;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AdminBugManageGUI implements InventoryHolder {

    private static final int SIZE = 27;

    private final Inventory inventory;
    private final TurtleGopyCore core;
    private final UUID reportId;

    public AdminBugManageGUI(TurtleGopyCore core, Player admin, UUID reportId) {
        this.core = core;
        this.reportId = reportId;

        BugReport report = core.getBugReportManager().getReport(reportId);
        String title = core.colorize("&4&l🐛 Báo Lỗi: &f" +
                (report != null ? report.getPlayerName() : "Unknown"));
        this.inventory = Bukkit.createInventory(this, SIZE, title);

        setupItems(report);
    }

    public static void open(TurtleGopyCore core, Player admin, UUID reportId) {
        AdminBugManageGUI gui = new AdminBugManageGUI(core, admin, reportId);
        admin.openInventory(gui.getInventory());
    }

    private void setupItems(BugReport report) {
        ItemStack bg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, bg);
        }

        if (report == null) return;

        // Report info
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§8────────────────────");
        infoLore.add("§7Người báo: §f" + report.getPlayerName());
        infoLore.add("§7Ngày báo: §f" + core.getBugReportManager().formatDate(report.getCreatedAt()));
        infoLore.add("§7Trạng thái: " + report.getStatus().getDisplayName());
        infoLore.add("§8────────────────────");

        String content = report.getContent();
        int lineLen = 40;
        for (int i = 0; i < content.length(); i += lineLen) {
            String line = content.substring(i, Math.min(i + lineLen, content.length()));
            infoLore.add("§f" + line);
        }

        if (report.getAdminNote() != null && !report.getAdminNote().isEmpty()) {
            infoLore.add("§8────────────────────");
            infoLore.add("§7Ghi chú: §e" + report.getAdminNote());
        }

        infoLore.add("§8────────────────────");

        ItemStack infoItem = createItem(Material.PAPER, "§c§lChi Tiết Báo Lỗi", infoLore);
        inventory.setItem(4, infoItem);

        // Action buttons
        ItemStack readBtn = createItem(Material.ENDER_EYE,
                "§b§lĐánh Dấu Đã Đọc",
                List.of("§7Đánh dấu báo lỗi là đã đọc",
                        report.getStatus() == BugReportStatus.READ ? "§a➤ Đang ở trạng thái này" : "§e➤ Click để chọn"));
        inventory.setItem(10, readBtn);

        ItemStack fixingBtn = createItem(Material.ANVIL,
                "§6§lĐang Sửa",
                List.of("§7Đánh dấu đang trong quá trình sửa",
                        report.getStatus() == BugReportStatus.FIXING ? "§a➤ Đang ở trạng thái này" : "§e➤ Click để chọn"));
        inventory.setItem(11, fixingBtn);

        ItemStack fixedBtn = createItem(Material.EMERALD,
                "§a§lĐã Sửa",
                List.of("§7Đánh dấu lỗi đã được sửa",
                        "§7Sẽ trao thưởng cho người báo",
                        report.getStatus() == BugReportStatus.FIXED ? "§a➤ Đang ở trạng thái này" : "§e➤ Click để chọn"));
        inventory.setItem(12, fixedBtn);

        ItemStack rejectBtn = createItem(Material.BARRIER,
                "§c§lKhông Phải Lỗi",
                List.of("§7Đánh dấu không phải lỗi",
                        report.getStatus() == BugReportStatus.REJECTED ? "§a➤ Đang ở trạng thái này" : "§e➤ Click để chọn"));
        inventory.setItem(13, rejectBtn);

        ItemStack deleteBtn = createItem(Material.TNT,
                "§4§lXóa Báo Lỗi",
                List.of("§c⚠ Hành động này không thể hoàn tác!",
                        "§e➤ Click để xóa"));
        inventory.setItem(16, deleteBtn);

        ItemStack backBtn = createItem(Material.ARROW,
                "§e◀ Quay Lại",
                List.of("§7Quay lại danh sách báo lỗi"));
        inventory.setItem(22, backBtn);
    }

    public static void handleClick(TurtleGopyCore core, InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AdminBugManageGUI gui)) return;

        event.setCancelled(true);

        Player admin = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        switch (slot) {
            case 10 -> {
                core.getBugReportManager().updateStatus(gui.reportId, BugReportStatus.READ);
                admin.sendMessage(core.getMessage("admin-bugreport-status-updated")
                        .replace("{status}", BugReportStatus.READ.getDisplayName()));
                open(core, admin, gui.reportId);
            }
            case 11 -> {
                core.getBugReportManager().updateStatus(gui.reportId, BugReportStatus.FIXING);
                admin.sendMessage(core.getMessage("admin-bugreport-status-updated")
                        .replace("{status}", BugReportStatus.FIXING.getDisplayName()));
                open(core, admin, gui.reportId);
            }
            case 12 -> {
                core.getBugReportManager().updateStatus(gui.reportId, BugReportStatus.FIXED);
                admin.sendMessage(core.getMessage("admin-bugreport-status-updated")
                        .replace("{status}", BugReportStatus.FIXED.getDisplayName()));
                open(core, admin, gui.reportId);
            }
            case 13 -> {
                core.getBugReportManager().updateStatus(gui.reportId, BugReportStatus.REJECTED);
                admin.sendMessage(core.getMessage("admin-bugreport-status-updated")
                        .replace("{status}", BugReportStatus.REJECTED.getDisplayName()));
                open(core, admin, gui.reportId);
            }
            case 16 -> {
                core.getBugReportManager().deleteReport(gui.reportId);
                admin.sendMessage(core.getMessage("admin-bugreport-deleted")
                        .replace("{id}", gui.reportId.toString().substring(0, 8)));
                admin.closeInventory();
                AdminBugReportGUI.open(core, admin, 0, null);
            }
            case 22 -> {
                admin.closeInventory();
                AdminBugReportGUI.open(core, admin, 0, null);
            }
        }
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(line);
        }
        return createItem(material, name, loreList);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
