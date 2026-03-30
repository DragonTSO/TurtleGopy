package com.turtle.turtlegopy.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.turtle.turtlegopy.api.model.Feedback;
import com.turtle.turtlegopy.api.model.FeedbackStatus;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AdminManageGUI implements InventoryHolder {

    private static final int SIZE = 27;

    private final Inventory inventory;
    private final TurtleGopyCore core;
    private final UUID feedbackId;

    public AdminManageGUI(TurtleGopyCore core, Player admin, UUID feedbackId) {
        this.core = core;
        this.feedbackId = feedbackId;

        Feedback feedback = core.getFeedbackManager().getFeedback(feedbackId);
        String title = core.colorize("&c&l⚙ Quản Lý: &f" +
                (feedback != null ? feedback.getPlayerName() : "Unknown"));
        this.inventory = Bukkit.createInventory(this, SIZE, title);

        setupItems(feedback);
    }

    public static void open(TurtleGopyCore core, Player admin, UUID feedbackId) {
        AdminManageGUI gui = new AdminManageGUI(core, admin, feedbackId);
        admin.openInventory(gui.getInventory());
    }

    private void setupItems(Feedback feedback) {
        // Fill background
        ItemStack bg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, bg);
        }

        if (feedback == null) return;

        // Feedback info (center top)
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§8────────────────────");
        infoLore.add("§7Người gửi: §f" + feedback.getPlayerName());
        infoLore.add("§7Ngày tạo: §f" + core.getFeedbackManager().formatDate(feedback.getCreatedAt()));
        infoLore.add("§7Trạng thái: " + feedback.getStatus().getDisplayName());
        infoLore.add("§8────────────────────");

        // Split content into lines
        String content = feedback.getContent();
        int lineLen = 40;
        for (int i = 0; i < content.length(); i += lineLen) {
            String line = content.substring(i, Math.min(i + lineLen, content.length()));
            infoLore.add("§f" + line);
        }

        if (feedback.getAdminNote() != null && !feedback.getAdminNote().isEmpty()) {
            infoLore.add("§8────────────────────");
            infoLore.add("§7Ghi chú: §e" + feedback.getAdminNote());
        }

        infoLore.add("§8────────────────────");

        ItemStack infoItem = createItem(Material.PAPER, "§6§lNội Dung Góp Ý", infoLore);
        inventory.setItem(4, infoItem);

        // Action buttons (middle row)
        // Mark as READ
        ItemStack readBtn = createItem(Material.ENDER_EYE,
                "§b§lĐánh Dấu Đã Đọc",
                List.of("§7Đánh dấu góp ý là đã đọc",
                        feedback.getStatus() == FeedbackStatus.READ ? "§a➤ Đang ở trạng thái này" : "§e➤ Click để chọn"));
        inventory.setItem(10, readBtn);

        // Accept
        ItemStack acceptBtn = createItem(Material.EMERALD,
                "§a§lChấp Nhận",
                List.of("§7Chấp nhận góp ý này",
                        "§7Sẽ trao thưởng cho người chơi",
                        feedback.getStatus() == FeedbackStatus.ACCEPTED ? "§a➤ Đang ở trạng thái này" : "§e➤ Click để chọn"));
        inventory.setItem(11, acceptBtn);

        // Reject
        ItemStack rejectBtn = createItem(Material.BARRIER,
                "§c§lTừ Chối",
                List.of("§7Từ chối góp ý này",
                        feedback.getStatus() == FeedbackStatus.REJECTED ? "§a➤ Đang ở trạng thái này" : "§e➤ Click để chọn"));
        inventory.setItem(12, rejectBtn);

        // Deploying
        ItemStack deployBtn = createItem(Material.CRAFTING_TABLE,
                "§6§lĐang Triển Khai",
                List.of("§7Đánh dấu góp ý đang được triển khai",
                        feedback.getStatus() == FeedbackStatus.DEPLOYING ? "§a➤ Đang ở trạng thái này" : "§e➤ Click để chọn"));
        inventory.setItem(13, deployBtn);

        // Implemented
        ItemStack implBtn = createItem(Material.NETHER_STAR,
                "§d§lĐã Triển Khai",
                List.of("§7Đánh dấu góp ý đã được triển khai",
                        feedback.getStatus() == FeedbackStatus.IMPLEMENTED ? "§a➤ Đang ở trạng thái này" : "§e➤ Click để chọn"));
        inventory.setItem(14, implBtn);

        // Delete
        ItemStack deleteBtn = createItem(Material.TNT,
                "§4§lXóa Góp Ý",
                List.of("§c⚠ Hành động này không thể hoàn tác!",
                        "§e➤ Click để xóa"));
        inventory.setItem(16, deleteBtn);

        // Back button
        ItemStack backBtn = createItem(Material.ARROW,
                "§e◀ Quay Lại",
                List.of("§7Quay lại danh sách góp ý"));
        inventory.setItem(22, backBtn);
    }

    public static void handleClick(TurtleGopyCore core, InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AdminManageGUI gui)) return;

        event.setCancelled(true);

        Player admin = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        switch (slot) {
            case 10 -> {
                // Mark as READ
                core.getFeedbackManager().updateStatus(gui.feedbackId, FeedbackStatus.READ);
                admin.sendMessage(core.getMessage("admin-status-updated")
                        .replace("{status}", FeedbackStatus.READ.getDisplayName()));
                open(core, admin, gui.feedbackId);
            }
            case 11 -> {
                // Accept
                core.getFeedbackManager().updateStatus(gui.feedbackId, FeedbackStatus.ACCEPTED);
                admin.sendMessage(core.getMessage("admin-status-updated")
                        .replace("{status}", FeedbackStatus.ACCEPTED.getDisplayName()));
                open(core, admin, gui.feedbackId);
            }
            case 12 -> {
                // Reject
                core.getFeedbackManager().updateStatus(gui.feedbackId, FeedbackStatus.REJECTED);
                admin.sendMessage(core.getMessage("admin-status-updated")
                        .replace("{status}", FeedbackStatus.REJECTED.getDisplayName()));
                open(core, admin, gui.feedbackId);
            }
            case 13 -> {
                // Deploying
                core.getFeedbackManager().updateStatus(gui.feedbackId, FeedbackStatus.DEPLOYING);
                admin.sendMessage(core.getMessage("admin-status-updated")
                        .replace("{status}", FeedbackStatus.DEPLOYING.getDisplayName()));
                open(core, admin, gui.feedbackId);
            }
            case 14 -> {
                // Implemented
                core.getFeedbackManager().updateStatus(gui.feedbackId, FeedbackStatus.IMPLEMENTED);
                admin.sendMessage(core.getMessage("admin-status-updated")
                        .replace("{status}", FeedbackStatus.IMPLEMENTED.getDisplayName()));
                open(core, admin, gui.feedbackId);
            }
            case 16 -> {
                // Delete
                core.getFeedbackManager().deleteFeedback(gui.feedbackId);
                admin.sendMessage(core.getMessage("admin-feedback-deleted")
                        .replace("{id}", gui.feedbackId.toString().substring(0, 8)));
                admin.closeInventory();
                AdminGUI.open(core, admin, 0, null);
            }
            case 22 -> {
                // Back
                admin.closeInventory();
                AdminGUI.open(core, admin, 0, null);
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
