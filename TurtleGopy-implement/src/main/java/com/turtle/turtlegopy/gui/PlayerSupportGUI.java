package com.turtle.turtlegopy.gui;

import java.util.ArrayList;
import java.util.List;

import com.turtle.turtlegopy.api.model.SupportTicket;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PlayerSupportGUI implements InventoryHolder {

    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;
    private static final int ITEMS_PER_PAGE = 28;

    private final Inventory inventory;
    private final TurtleGopyCore core;
    private final Player player;
    private final int page;

    public PlayerSupportGUI(TurtleGopyCore core, Player player, int page) {
        this.core = core;
        this.player = player;
        this.page = page;

        String title = core.colorize(
                core.getPlugin().getConfig().getString("gui.support-player.title", "&3&l🎫 Phiếu Hỗ Trợ Của Bạn"));
        this.inventory = Bukkit.createInventory(this, SIZE, title);

        setupItems();
    }

    public static void open(TurtleGopyCore core, Player player, int page) {
        PlayerSupportGUI gui = new PlayerSupportGUI(core, player, page);
        player.openInventory(gui.getInventory());
    }

    private void setupItems() {
        fillBorder();

        List<SupportTicket> tickets = core.getSupportTicketManager().getPlayerTickets(player.getUniqueId());
        tickets.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        int totalPages = Math.max(1, (int) Math.ceil((double) tickets.size() / ITEMS_PER_PAGE));
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, tickets.size());

        int[] slots = getContentSlots();
        int slotIndex = 0;

        for (int i = start; i < end && slotIndex < slots.length; i++) {
            SupportTicket ticket = tickets.get(i);
            inventory.setItem(slots[slotIndex], createTicketItem(ticket));
            slotIndex++;
        }

        // Create ticket button
        ItemStack createBtn = createItem(Material.WRITABLE_BOOK,
                "§3§l🎫 Tạo Phiếu Hỗ Trợ Mới",
                "§7Click để tạo phiếu hỗ trợ mới",
                "§7Bạn sẽ gõ nội dung vào chat",
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
                "§3§lThông Tin Hỗ Trợ",
                "§7Tổng phiếu: §e" + tickets.size(),
                "§7Trang: §e" + (page + 1) + "/" + totalPages,
                "",
                "§7Trạng thái:",
                "§e⏳ Chờ xử lý",
                "§b👁 Đã đọc",
                "§6🔄 Đang xử lý",
                "§a✔ Đã giải quyết",
                "§c✘ Từ chối");
        inventory.setItem(4, infoItem);
    }

    private ItemStack createTicketItem(SupportTicket ticket) {
        String contentPreview = ticket.getContent().length() > 40
                ? ticket.getContent().substring(0, 40) + "..."
                : ticket.getContent();

        List<String> lore = new ArrayList<>();
        lore.add("§8────────────────────");
        lore.add("§7Nội dung: §f" + contentPreview);
        lore.add("§7Trạng thái: " + ticket.getStatus().getDisplayName());
        lore.add("§7Ngày tạo: §f" + core.getSupportTicketManager().formatDate(ticket.getCreatedAt()));

        if (ticket.getAdminNote() != null && !ticket.getAdminNote().isEmpty()) {
            lore.add("");
            lore.add("§7Phản hồi admin: §f" + ticket.getAdminNote());
        }

        if (ticket.isRewardGiven()) {
            lore.add("");
            lore.add("§a✔ Đã nhận thưởng");
        }

        lore.add("§8────────────────────");
        lore.add("§7ID: §8" + ticket.getId().toString().substring(0, 8));
        lore.add("");
        lore.add("§e➤ Click để mở chat hỗ trợ");

        ItemStack item = new ItemStack(ticket.getStatus().getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ticket.getStatus().getColor() + "§l" + contentPreview);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private void fillBorder() {
        ItemStack border = createItem(Material.CYAN_STAINED_GLASS_PANE, " ");
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
        if (!(event.getInventory().getHolder() instanceof PlayerSupportGUI gui)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 49) {
            if (core.getSupportTicketManager().hasActiveTicket(player.getUniqueId())) {
                player.sendMessage(core.getMessage("support-already-active"));
                return;
            }
            player.closeInventory();
            core.getChatInputListener().startSupportInput(player);
            player.sendMessage(core.getMessage("support-input-prompt"));
            return;
        }

        if (slot == 45 && gui.page > 0) {
            open(core, player, gui.page - 1);
            return;
        }

        if (slot == 53) {
            List<SupportTicket> tickets = core.getSupportTicketManager().getPlayerTickets(player.getUniqueId());
            int totalPages = Math.max(1, (int) Math.ceil((double) tickets.size() / ITEMS_PER_PAGE));
            if (gui.page < totalPages - 1) {
                open(core, player, gui.page + 1);
            }
            return;
        }

        // Content slot clicked - open chat for that ticket
        int[] contentSlots = gui.getContentSlots();
        int contentIndex = -1;
        for (int i = 0; i < contentSlots.length; i++) {
            if (contentSlots[i] == slot) {
                contentIndex = i;
                break;
            }
        }

        if (contentIndex >= 0) {
            List<SupportTicket> tickets = core.getSupportTicketManager().getPlayerTickets(player.getUniqueId());
            tickets.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
            int ticketIndex = gui.page * ITEMS_PER_PAGE + contentIndex;
            if (ticketIndex < tickets.size()) {
                SupportTicket ticket = tickets.get(ticketIndex);
                player.closeInventory();
                core.getSupportChatManager().enterChat(player, ticket.getId());
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
