package com.turtle.turtlegopy.gui;

import java.util.ArrayList;
import java.util.List;

import com.turtle.turtlegopy.api.model.BugReport;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PlayerBugReportGUI implements InventoryHolder {

    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;
    private static final int ITEMS_PER_PAGE = 28;

    private final Inventory inventory;
    private final TurtleGopyCore core;
    private final Player player;
    private final int page;

    public PlayerBugReportGUI(TurtleGopyCore core, Player player, int page) {
        this.core = core;
        this.player = player;
        this.page = page;

        String title = core.colorize(
                core.getPlugin().getConfig().getString("gui.bugreport-player.title", "&c&l🐛 Báo Lỗi Của Bạn"));
        this.inventory = Bukkit.createInventory(this, SIZE, title);

        setupItems();
    }

    public static void open(TurtleGopyCore core, Player player, int page) {
        PlayerBugReportGUI gui = new PlayerBugReportGUI(core, player, page);
        player.openInventory(gui.getInventory());
    }

    private void setupItems() {
        fillBorder();

        List<BugReport> reports = core.getBugReportManager().getPlayerReports(player.getUniqueId());
        reports.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        int totalPages = Math.max(1, (int) Math.ceil((double) reports.size() / ITEMS_PER_PAGE));
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, reports.size());

        int[] slots = getContentSlots();
        int slotIndex = 0;

        for (int i = start; i < end && slotIndex < slots.length; i++) {
            BugReport report = reports.get(i);
            inventory.setItem(slots[slotIndex], createReportItem(report));
            slotIndex++;
        }

        // Create bug report button
        ItemStack createBtn = createItem(Material.WRITABLE_BOOK,
                "§c§l🐛 Báo Lỗi Mới",
                "§7Click để báo lỗi mới",
                "§7Bạn sẽ gõ mô tả lỗi vào chat",
                "",
                "§e➤ Click để tạo!");
        inventory.setItem(49, createBtn);

        // Navigation
        if (page > 0) {
            inventory.setItem(45, createItem(Material.ARROW,
                    "§e◀ Trang trước",
                    "§7Trang " + page + "/" + totalPages));
        }

        if (page < totalPages - 1) {
            inventory.setItem(53, createItem(Material.ARROW,
                    "§e▶ Trang sau",
                    "§7Trang " + (page + 2) + "/" + totalPages));
        }

        // Info
        ItemStack infoItem = createItem(Material.BOOK,
                "§c§lThông Tin Báo Lỗi",
                "§7Tổng báo lỗi: §e" + reports.size(),
                "§7Trang: §e" + (page + 1) + "/" + totalPages,
                "",
                "§7Trạng thái:",
                "§e⏳ Chờ xử lý",
                "§b👁 Đã đọc",
                "§6🔧 Đang sửa",
                "§a✔ Đã sửa",
                "§c✘ Không phải lỗi");
        inventory.setItem(4, infoItem);
    }

    private ItemStack createReportItem(BugReport report) {
        String contentPreview = report.getContent().length() > 40
                ? report.getContent().substring(0, 40) + "..."
                : report.getContent();

        List<String> lore = new ArrayList<>();
        lore.add("§8────────────────────");
        lore.add("§7Mô tả: §f" + contentPreview);
        lore.add("§7Trạng thái: " + report.getStatus().getDisplayName());
        lore.add("§7Ngày báo: §f" + core.getBugReportManager().formatDate(report.getCreatedAt()));

        if (report.getAdminNote() != null && !report.getAdminNote().isEmpty()) {
            lore.add("");
            lore.add("§7Ghi chú admin: §f" + report.getAdminNote());
        }

        if (report.isRewardGiven()) {
            lore.add("");
            lore.add("§a✔ Đã nhận thưởng");
        }

        lore.add("§8────────────────────");
        lore.add("§7ID: §8" + report.getId().toString().substring(0, 8));

        ItemStack item = new ItemStack(report.getStatus().getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(report.getStatus().getColor() + "§l" + contentPreview);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private void fillBorder() {
        ItemStack border = createItem(Material.RED_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == ROWS - 1 || col == 0 || col == 8) {
                inventory.setItem(i, border);
            }
        }
    }

    private int[] getContentSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row < ROWS - 1; row++) {
            for (int col = 1; col < 8; col++) {
                slots.add(row * 9 + col);
            }
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(line);
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void handleClick(TurtleGopyCore core, InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PlayerBugReportGUI gui)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 49) {
            player.closeInventory();
            core.getChatInputListener().startBugReportInput(player);
            player.sendMessage(core.getMessage("bugreport-input-prompt"));
            return;
        }

        if (slot == 45 && gui.page > 0) {
            open(core, player, gui.page - 1);
            return;
        }

        if (slot == 53) {
            List<BugReport> reports = core.getBugReportManager().getPlayerReports(player.getUniqueId());
            int totalPages = Math.max(1, (int) Math.ceil((double) reports.size() / ITEMS_PER_PAGE));
            if (gui.page < totalPages - 1) {
                open(core, player, gui.page + 1);
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
