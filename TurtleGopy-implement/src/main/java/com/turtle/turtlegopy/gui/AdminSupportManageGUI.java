package com.turtle.turtlegopy.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.turtle.turtlegopy.api.model.SupportTicket;
import com.turtle.turtlegopy.api.model.SupportTicketStatus;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AdminSupportManageGUI implements InventoryHolder {

    private static final int SIZE = 27;

    private final Inventory inventory;
    private final TurtleGopyCore core;
    private final UUID ticketId;

    public AdminSupportManageGUI(TurtleGopyCore core, Player admin, UUID ticketId) {
        this.core = core;
        this.ticketId = ticketId;

        SupportTicket ticket = core.getSupportTicketManager().getTicket(ticketId);
        String title = core.colorize("&3&l🎫 Hỗ Trợ: &f" +
                (ticket != null ? ticket.getPlayerName() : "Unknown"));
        this.inventory = Bukkit.createInventory(this, SIZE, title);

        setupItems(ticket);
    }

    public static void open(TurtleGopyCore core, Player admin, UUID ticketId) {
        AdminSupportManageGUI gui = new AdminSupportManageGUI(core, admin, ticketId);
        admin.openInventory(gui.getInventory());
    }

    private void setupItems(SupportTicket ticket) {
        ItemStack bg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, bg);
        }

        if (ticket == null) return;

        // Ticket info
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§8────────────────────");
        infoLore.add("§7Người gửi: §f" + ticket.getPlayerName());
        infoLore.add("§7Ngày tạo: §f" + core.getSupportTicketManager().formatDate(ticket.getCreatedAt()));
        infoLore.add("§7Trạng thái: " + ticket.getStatus().getDisplayName());
        infoLore.add("§8────────────────────");

        String content = ticket.getContent();
        int lineLen = 40;
        for (int i = 0; i < content.length(); i += lineLen) {
            String line = content.substring(i, Math.min(i + lineLen, content.length()));
            infoLore.add("§f" + line);
        }

        if (ticket.getAdminNote() != null && !ticket.getAdminNote().isEmpty()) {
            infoLore.add("§8────────────────────");
            infoLore.add("§7Ghi chú: §e" + ticket.getAdminNote());
        }

        infoLore.add("§8────────────────────");

        ItemStack infoItem = createItem(Material.PAPER, "§3§lChi Tiết Hỗ Trợ", infoLore);
        inventory.setItem(4, infoItem);

        // Action buttons
        ItemStack readBtn = createItem(Material.ENDER_EYE,
                "§b§lĐánh Dấu Đã Đọc",
                List.of("§7Đánh dấu phiếu hỗ trợ là đã đọc",
                        ticket.getStatus() == SupportTicketStatus.READ ? "§a➤ Đang ở trạng thái này" : "§e➤ Click để chọn"));
        inventory.setItem(10, readBtn);

        ItemStack processingBtn = createItem(Material.ANVIL,
                "§6§lĐang Xử Lý",
                List.of("§7Đánh dấu đang trong quá trình xử lý",
                        ticket.getStatus() == SupportTicketStatus.PROCESSING ? "§a➤ Đang ở trạng thái này" : "§e➤ Click để chọn"));
        inventory.setItem(11, processingBtn);

        ItemStack resolvedBtn = createItem(Material.EMERALD,
                "§a§lĐã Giải Quyết",
                List.of("§7Đánh dấu phiếu đã được giải quyết",
                        ticket.getStatus() == SupportTicketStatus.RESOLVED ? "§a➤ Đang ở trạng thái này" : "§e➤ Click để chọn"));
        inventory.setItem(12, resolvedBtn);

        ItemStack rejectBtn = createItem(Material.BARRIER,
                "§c§lTừ Chối",
                List.of("§7Từ chối phiếu hỗ trợ này",
                        ticket.getStatus() == SupportTicketStatus.REJECTED ? "§a➤ Đang ở trạng thái này" : "§e➤ Click để chọn"));
        inventory.setItem(13, rejectBtn);

        ItemStack deleteBtn = createItem(Material.TNT,
                "§4§lXóa Phiếu",
                List.of("§c⚠ Hành động này không thể hoàn tác!",
                        "§e➤ Click để xóa"));
        inventory.setItem(16, deleteBtn);

        ItemStack backBtn = createItem(Material.ARROW,
                "§e◀ Quay Lại",
                List.of("§7Quay lại danh sách hỗ trợ"));
        inventory.setItem(22, backBtn);

        // Open Chat button
        ItemStack chatBtn = createItem(Material.NAME_TAG,
                "§3§l💬 Mở Chat",
                List.of("§7Vào luồng chat riêng với người chơi",
                        "§7Để hỗ trợ trực tiếp",
                        "",
                        "§e➤ Click để mở chat"));
        inventory.setItem(15, chatBtn);
    }

    public static void handleClick(TurtleGopyCore core, InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AdminSupportManageGUI gui)) return;

        event.setCancelled(true);

        Player admin = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        switch (slot) {
            case 10 -> {
                core.getSupportTicketManager().updateStatus(gui.ticketId, SupportTicketStatus.READ);
                admin.sendMessage(core.getMessage("admin-support-status-updated")
                        .replace("{status}", SupportTicketStatus.READ.getDisplayName()));
                open(core, admin, gui.ticketId);
            }
            case 11 -> {
                core.getSupportTicketManager().updateStatus(gui.ticketId, SupportTicketStatus.PROCESSING);
                admin.sendMessage(core.getMessage("admin-support-status-updated")
                        .replace("{status}", SupportTicketStatus.PROCESSING.getDisplayName()));
                open(core, admin, gui.ticketId);
            }
            case 12 -> {
                core.getSupportTicketManager().updateStatus(gui.ticketId, SupportTicketStatus.RESOLVED);
                admin.sendMessage(core.getMessage("admin-support-status-updated")
                        .replace("{status}", SupportTicketStatus.RESOLVED.getDisplayName()));
                open(core, admin, gui.ticketId);
            }
            case 13 -> {
                core.getSupportTicketManager().updateStatus(gui.ticketId, SupportTicketStatus.REJECTED);
                admin.sendMessage(core.getMessage("admin-support-status-updated")
                        .replace("{status}", SupportTicketStatus.REJECTED.getDisplayName()));
                open(core, admin, gui.ticketId);
            }
            case 15 -> {
                // Open Chat
                admin.closeInventory();
                core.getSupportChatManager().enterChat(admin, gui.ticketId);
            }
            case 16 -> {
                core.getSupportTicketManager().deleteTicket(gui.ticketId);
                admin.sendMessage(core.getMessage("admin-support-deleted")
                        .replace("{id}", gui.ticketId.toString().substring(0, 8)));
                admin.closeInventory();
                AdminSupportGUI.open(core, admin, 0, null);
            }
            case 22 -> {
                admin.closeInventory();
                AdminSupportGUI.open(core, admin, 0, null);
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
