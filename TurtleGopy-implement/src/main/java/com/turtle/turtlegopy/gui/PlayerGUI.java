package com.turtle.turtlegopy.gui;

import java.util.ArrayList;
import java.util.List;

import com.turtle.turtlegopy.api.model.Feedback;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PlayerGUI implements InventoryHolder, Listener {

    private static final String GUI_ID = "turtlegopy_player";
    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;
    private static final int ITEMS_PER_PAGE = 28;

    private final Inventory inventory;
    private final TurtleGopyCore core;
    private final Player player;
    private final int page;

    public PlayerGUI(TurtleGopyCore core, Player player, int page) {
        this.core = core;
        this.player = player;
        this.page = page;

        String title = core.colorize(
                core.getPlugin().getConfig().getString("gui.player.title", "&6&l✉ Góp Ý Của Bạn"));
        this.inventory = Bukkit.createInventory(this, SIZE, title);

        setupItems();
    }

    public static void open(TurtleGopyCore core, Player player, int page) {
        PlayerGUI gui = new PlayerGUI(core, player, page);
        player.openInventory(gui.getInventory());
    }

    private void setupItems() {
        fillBorder();

        List<Feedback> feedbacks = core.getFeedbackManager().getPlayerFeedbacks(player.getUniqueId());
        feedbacks.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        int totalPages = Math.max(1, (int) Math.ceil((double) feedbacks.size() / ITEMS_PER_PAGE));
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, feedbacks.size());

        int[] slots = getContentSlots();
        int slotIndex = 0;

        for (int i = start; i < end && slotIndex < slots.length; i++) {
            Feedback feedback = feedbacks.get(i);
            inventory.setItem(slots[slotIndex], createFeedbackItem(feedback));
            slotIndex++;
        }

        // Create feedback button
        ItemStack createBtn = createItem(Material.WRITABLE_BOOK,
                "§a§l✉ Tạo Góp Ý Mới",
                "§7Click để tạo góp ý mới",
                "§7Bạn sẽ gõ nội dung vào chat",
                "",
                "§e➤ Click để tạo!");
        inventory.setItem(49, createBtn);

        // Navigation
        if (page > 0) {
            ItemStack prevBtn = createItem(Material.ARROW,
                    "§e◀ Trang trước",
                    "§7Trang " + page + "/" + totalPages);
            inventory.setItem(45, prevBtn);
        }

        if (page < totalPages - 1) {
            ItemStack nextBtn = createItem(Material.ARROW,
                    "§e▶ Trang sau",
                    "§7Trang " + (page + 2) + "/" + totalPages);
            inventory.setItem(53, nextBtn);
        }

        // Info item
        ItemStack infoItem = createItem(Material.BOOK,
                "§6§lThông Tin",
                "§7Tổng góp ý: §e" + feedbacks.size(),
                "§7Trang: §e" + (page + 1) + "/" + totalPages,
                "",
                "§7Trạng thái góp ý:",
                "§e⏳ Chờ xử lý",
                "§b👁 Đã đọc",
                "§a✔ Đã chấp nhận",
                "§c✘ Từ chối",
                "§d⚡ Đã triển khai");
        inventory.setItem(4, infoItem);
    }

    private ItemStack createFeedbackItem(Feedback feedback) {
        String contentPreview = feedback.getContent().length() > 40
                ? feedback.getContent().substring(0, 40) + "..."
                : feedback.getContent();

        List<String> lore = new ArrayList<>();
        lore.add("§8────────────────────");
        lore.add("§7Nội dung: §f" + contentPreview);
        lore.add("§7Trạng thái: " + feedback.getStatus().getDisplayName());
        lore.add("§7Ngày tạo: §f" + core.getFeedbackManager().formatDate(feedback.getCreatedAt()));

        if (feedback.getAdminNote() != null && !feedback.getAdminNote().isEmpty()) {
            lore.add("");
            lore.add("§7Ghi chú admin: §f" + feedback.getAdminNote());
        }

        if (feedback.isRewardGiven()) {
            lore.add("");
            lore.add("§a✔ Đã nhận thưởng");
        }

        lore.add("§8────────────────────");
        lore.add("§7ID: §8" + feedback.getId().toString().substring(0, 8));
        lore.add("");
        lore.add("§3💬 Click để mở chat với admin");

        ItemStack item = new ItemStack(feedback.getStatus().getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(feedback.getStatus().getColor() + "§l" + contentPreview);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private void fillBorder() {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
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
        if (!(event.getInventory().getHolder() instanceof PlayerGUI gui)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 49) {
            // Create new feedback
            player.closeInventory();
            core.getChatInputListener().startInput(player);
            player.sendMessage(core.getMessage("feedback-input-prompt"));
            return;
        }

        if (slot == 45 && gui.page > 0) {
            open(core, player, gui.page - 1);
            return;
        }

        if (slot == 53) {
            List<Feedback> feedbacks = core.getFeedbackManager().getPlayerFeedbacks(player.getUniqueId());
            int totalPages = Math.max(1, (int) Math.ceil((double) feedbacks.size() / ITEMS_PER_PAGE));
            if (gui.page < totalPages - 1) {
                open(core, player, gui.page + 1);
            }
            return;
        }

        // Handle clicking on a feedback item → open chat
        int[] contentSlots = gui.getContentSlots();
        int clickedIndex = -1;
        for (int i = 0; i < contentSlots.length; i++) {
            if (contentSlots[i] == slot) {
                clickedIndex = i;
                break;
            }
        }

        if (clickedIndex >= 0) {
            List<Feedback> feedbacks = core.getFeedbackManager().getPlayerFeedbacks(player.getUniqueId());
            feedbacks.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
            int actualIndex = gui.page * ITEMS_PER_PAGE + clickedIndex;
            if (actualIndex < feedbacks.size()) {
                Feedback feedback = feedbacks.get(actualIndex);
                player.closeInventory();
                core.getSupportChatManager().enterGenericChat(player, feedback.getId());
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
