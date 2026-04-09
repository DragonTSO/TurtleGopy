package com.turtle.turtlegopy.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.turtle.turtlegopy.api.model.BugReport;
import com.turtle.turtlegopy.api.model.Feedback;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RewardListGUI implements InventoryHolder {

    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;
    private static final int ITEMS_PER_PAGE = 28;

    private final Inventory inventory;
    private final TurtleGopyCore core;
    private final Player player;
    private final int page;

    // Store reward entry references for click handling
    private final List<RewardEntry> rewardEntries = new ArrayList<>();

    public RewardListGUI(TurtleGopyCore core, Player player, int page) {
        this.core = core;
        this.player = player;
        this.page = page;

        String title = core.colorize("&6&l🎁 Phần Thưởng Chờ Nhận");
        this.inventory = Bukkit.createInventory(this, SIZE, title);

        setupItems();
    }

    public static void open(TurtleGopyCore core, Player player, int page) {
        RewardListGUI gui = new RewardListGUI(core, player, page);
        player.openInventory(gui.getInventory());
    }

    private void setupItems() {
        fillBorder();

        // Collect all pending rewards
        List<Feedback> pendingFeedbacks = core.getFeedbackManager().getPendingRewards(player.getUniqueId());
        List<BugReport> pendingReports = core.getBugReportManager().getPendingRewards(player.getUniqueId());

        // Build unified list
        rewardEntries.clear();
        for (Feedback f : pendingFeedbacks) {
            rewardEntries.add(new RewardEntry(f.getId(), RewardType.FEEDBACK, f.getContent(), f.getCreatedAt()));
        }
        for (BugReport r : pendingReports) {
            rewardEntries.add(new RewardEntry(r.getId(), RewardType.BUG_REPORT, r.getContent(), r.getCreatedAt()));
        }

        // Sort by creation date (newest first)
        rewardEntries.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));

        int totalItems = rewardEntries.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE));
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, totalItems);

        int[] slots = getContentSlots();
        int slotIndex = 0;

        for (int i = start; i < end && slotIndex < slots.length; i++) {
            RewardEntry entry = rewardEntries.get(i);
            inventory.setItem(slots[slotIndex], createRewardItem(entry));
            slotIndex++;
        }

        // Info item
        ItemStack infoItem = createItem(Material.NETHER_STAR,
                "§6§lThông Tin",
                "§7Tổng phần thưởng chờ: §e" + totalItems,
                "§7Trang: §e" + (page + 1) + "/" + totalPages,
                "",
                "§7Click vào phần thưởng để chọn quà!");
        inventory.setItem(4, infoItem);

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
    }

    private ItemStack createRewardItem(RewardEntry entry) {
        String contentPreview = entry.content.length() > 35
                ? entry.content.substring(0, 35) + "..."
                : entry.content;

        boolean isFeedback = entry.type == RewardType.FEEDBACK;
        Material icon = isFeedback ? Material.EMERALD : Material.DIAMOND;
        String typeLabel = isFeedback ? "§a§l✉ Góp Ý" : "§c§l🐛 Báo Lỗi";
        String typeColor = isFeedback ? "§a" : "§c";
        String shortId = entry.id.toString().substring(0, 8);

        List<String> lore = new ArrayList<>();
        lore.add("§8────────────────────");
        lore.add("§7Loại: " + typeLabel);
        lore.add("§7ID: §e#" + shortId);
        lore.add("§7Nội dung: §f" + contentPreview);
        lore.add("§8────────────────────");
        lore.add("");
        lore.add("§e➤ Click để chọn quà!");

        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(typeColor + "§l🎁 " + typeLabel + " §7#" + shortId);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private void fillBorder() {
        ItemStack border = createItem(Material.ORANGE_STAINED_GLASS_PANE, " ");
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
        if (!(event.getInventory().getHolder() instanceof RewardListGUI gui)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        // Navigation
        if (slot == 45 && gui.page > 0) {
            open(core, player, gui.page - 1);
            return;
        }

        if (slot == 53) {
            int totalItems = gui.rewardEntries.size();
            int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE));
            if (gui.page < totalPages - 1) {
                open(core, player, gui.page + 1);
            }
            return;
        }

        // Check if clicked on a reward item
        int[] contentSlots = gui.getContentSlots();
        int clickedIndex = -1;
        for (int i = 0; i < contentSlots.length; i++) {
            if (contentSlots[i] == slot) {
                clickedIndex = i;
                break;
            }
        }

        if (clickedIndex >= 0) {
            int actualIndex = gui.page * ITEMS_PER_PAGE + clickedIndex;
            if (actualIndex < gui.rewardEntries.size()) {
                RewardEntry entry = gui.rewardEntries.get(actualIndex);
                // Open reward choose GUI
                RewardChooseGUI.open(core, player, entry.id, entry.type);
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    // Inner class for reward entries
    public enum RewardType {
        FEEDBACK, BUG_REPORT
    }

    public static class RewardEntry {
        public final UUID id;
        public final RewardType type;
        public final String content;
        public final long createdAt;

        public RewardEntry(UUID id, RewardType type, String content, long createdAt) {
            this.id = id;
            this.type = type;
            this.content = content;
            this.createdAt = createdAt;
        }
    }
}
