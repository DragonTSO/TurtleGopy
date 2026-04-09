package com.turtle.turtlegopy.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

public class AdminGUI implements InventoryHolder {

    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;
    private static final int ITEMS_PER_PAGE = 28;

    private final Inventory inventory;
    private final TurtleGopyCore core;
    private final Player admin;
    private final int page;
    private final FeedbackStatus filterStatus;
    private final String playerFilter;
    private List<Feedback> currentFeedbacks;

    public AdminGUI(TurtleGopyCore core, Player admin, int page, FeedbackStatus filterStatus) {
        this(core, admin, page, filterStatus, null);
    }

    public AdminGUI(TurtleGopyCore core, Player admin, int page, FeedbackStatus filterStatus, String playerFilter) {
        this.core = core;
        this.admin = admin;
        this.page = page;
        this.filterStatus = filterStatus;
        this.playerFilter = playerFilter;

        String title = core.colorize(
                core.getPlugin().getConfig().getString("gui.admin.title", "&6&l✉ Quản Lý Góp Ý"));
        if (playerFilter != null) {
            title += " §7[§f" + playerFilter + "§7]";
        } else if (filterStatus != null) {
            title += " §7[" + filterStatus.getDisplayName() + "§7]";
        }
        this.inventory = Bukkit.createInventory(this, SIZE, title);

        setupItems();
    }

    public static void open(TurtleGopyCore core, Player admin, int page, FeedbackStatus filter) {
        open(core, admin, page, filter, null);
    }

    public static void open(TurtleGopyCore core, Player admin, int page, FeedbackStatus filter, String playerFilter) {
        AdminGUI gui = new AdminGUI(core, admin, page, filter, playerFilter);
        admin.openInventory(gui.getInventory());
    }

    private void setupItems() {
        fillBorder();

        List<Feedback> allFeedbacks = core.getFeedbackManager().getAllFeedbacks();

        currentFeedbacks = new ArrayList<>(allFeedbacks);

        if (playerFilter != null && !playerFilter.isEmpty()) {
            currentFeedbacks.removeIf(f -> !f.getPlayerName().equalsIgnoreCase(playerFilter));
        }

        if (filterStatus != null) {
            currentFeedbacks.removeIf(f -> f.getStatus() != filterStatus);
        }

        currentFeedbacks.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        int totalPages = Math.max(1, (int) Math.ceil((double) currentFeedbacks.size() / ITEMS_PER_PAGE));
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, currentFeedbacks.size());

        int[] slots = getContentSlots();
        int slotIndex = 0;

        for (int i = start; i < end && slotIndex < slots.length; i++) {
            Feedback feedback = currentFeedbacks.get(i);
            inventory.setItem(slots[slotIndex], createFeedbackItem(feedback));
            slotIndex++;
        }

        // Filter buttons
        int filterSlot = 45;
        for (FeedbackStatus status : FeedbackStatus.values()) {
            if (filterSlot > 48) break;
            boolean isActive = filterStatus == status;
            ItemStack filterBtn = createItem(
                    isActive ? Material.LIME_DYE : Material.GRAY_DYE,
                    (isActive ? "§a§l" : "§7") + status.getDisplayName(),
                    "§7Click để lọc theo trạng thái này",
                    isActive ? "§a➤ Đang lọc" : "§8➤ Click để chọn");
            inventory.setItem(filterSlot, filterBtn);
            filterSlot++;
        }

        // Show all button
        ItemStack allBtn = createItem(
                filterStatus == null ? Material.LIME_DYE : Material.GRAY_DYE,
                filterStatus == null ? "§a§lTất Cả" : "§7Tất Cả",
                "§7Hiển thị tất cả góp ý",
                filterStatus == null ? "§a➤ Đang chọn" : "§8➤ Click để chọn");
        inventory.setItem(49, allBtn);

        // Info
        ItemStack infoItem = createItem(Material.BOOK,
                "§c§lQuản Lý Góp Ý",
                "§7Tổng: §e" + allFeedbacks.size(),
                "§7Đang hiển thị: §e" + currentFeedbacks.size(),
                "§7Trang: §e" + (page + 1) + "/" + totalPages,
                "",
                "§7Click vào góp ý để quản lý");
        inventory.setItem(4, infoItem);

        // Navigation
        if (page > 0) {
            inventory.setItem(50, createItem(Material.ARROW, "§e◀ Trang trước",
                    "§7Trang " + page + "/" + totalPages));
        }

        if (page < totalPages - 1) {
            inventory.setItem(53, createItem(Material.ARROW, "§e▶ Trang sau",
                    "§7Trang " + (page + 2) + "/" + totalPages));
        }
    }

    private ItemStack createFeedbackItem(Feedback feedback) {
        String contentPreview = feedback.getContent().length() > 40
                ? feedback.getContent().substring(0, 40) + "..."
                : feedback.getContent();

        List<String> lore = new ArrayList<>();
        lore.add("§8────────────────────");
        lore.add("§7Người gửi: §f" + feedback.getPlayerName());
        lore.add("§7Nội dung: §f" + contentPreview);
        lore.add("§7Trạng thái: " + feedback.getStatus().getDisplayName());
        lore.add("§7Ngày tạo: §f" + core.getFeedbackManager().formatDate(feedback.getCreatedAt()));

        if (feedback.getAdminNote() != null && !feedback.getAdminNote().isEmpty()) {
            lore.add("");
            lore.add("§7Ghi chú: §f" + feedback.getAdminNote());
        }

        if (feedback.isRewardGiven()) {
            lore.add("");
            lore.add("§a✔ Đã trao thưởng");
        }

        lore.add("§8────────────────────");
        lore.add("§e➤ Click để quản lý");

        ItemStack item = new ItemStack(feedback.getStatus().getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(feedback.getStatus().getColor() + "§l[" + feedback.getPlayerName() + "] " + contentPreview);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    public static void handleClick(TurtleGopyCore core, InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AdminGUI gui)) return;

        event.setCancelled(true);

        Player admin = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        // Filter buttons (45-48 = statuses, 49 = all)
        if (slot >= 45 && slot <= 48) {
            int statusIndex = slot - 45;
            FeedbackStatus[] statuses = FeedbackStatus.values();
            if (statusIndex < statuses.length) {
                FeedbackStatus clickedStatus = statuses[statusIndex];
                FeedbackStatus newFilter = (gui.filterStatus == clickedStatus) ? null : clickedStatus;
                open(core, admin, 0, newFilter, gui.playerFilter);
            }
            return;
        }

        if (slot == 49) {
            open(core, admin, 0, null, gui.playerFilter);
            return;
        }

        if (slot == 50 && gui.page > 0) {
            open(core, admin, gui.page - 1, gui.filterStatus, gui.playerFilter);
            return;
        }

        if (slot == 53) {
            int totalPages = Math.max(1, (int) Math.ceil((double) gui.currentFeedbacks.size() / ITEMS_PER_PAGE));
            if (gui.page < totalPages - 1) {
                open(core, admin, gui.page + 1, gui.filterStatus, gui.playerFilter);
            }
            return;
        }

        // Content slot clicked - open manage GUI
        int[] contentSlots = gui.getContentSlots();
        int contentIndex = -1;
        for (int i = 0; i < contentSlots.length; i++) {
            if (contentSlots[i] == slot) {
                contentIndex = i;
                break;
            }
        }

        if (contentIndex >= 0) {
            int feedbackIndex = gui.page * ITEMS_PER_PAGE + contentIndex;
            if (feedbackIndex < gui.currentFeedbacks.size()) {
                Feedback feedback = gui.currentFeedbacks.get(feedbackIndex);
                AdminManageGUI.open(core, admin, feedback.getId());
            }
        }
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

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
