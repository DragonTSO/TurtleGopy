package com.turtle.turtlegopy.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RewardChooseGUI implements InventoryHolder {

    private static final int SIZE = 27;

    private final Inventory inventory;
    private final TurtleGopyCore core;
    private final UUID rewardId;
    private final RewardListGUI.RewardType rewardType;

    public RewardChooseGUI(TurtleGopyCore core, Player player, UUID rewardId, RewardListGUI.RewardType rewardType) {
        this.core = core;
        this.rewardId = rewardId;
        this.rewardType = rewardType;

        String title = core.colorize("&6&l🎁 Chọn Phần Thưởng");
        this.inventory = Bukkit.createInventory(this, SIZE, title);

        setupItems();
    }

    public static void open(TurtleGopyCore core, Player player, UUID rewardId, RewardListGUI.RewardType rewardType) {
        RewardChooseGUI gui = new RewardChooseGUI(core, player, rewardId, rewardType);
        player.openInventory(gui.getInventory());
    }

    private void setupItems() {
        // Fill background
        ItemStack bg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, bg);
        }

        // Determine config path
        String configPath = rewardType == RewardListGUI.RewardType.FEEDBACK ? "rewards" : "bugreport-rewards";

        // Option A (slot 11)
        String nameA = core.colorize(core.getPlugin().getConfig().getString(configPath + ".option-a.name", "&ePhần thưởng A"));
        String iconAStr = core.getPlugin().getConfig().getString(configPath + ".option-a.icon", "AMETHYST_SHARD");
        Material iconA = parseMaterial(iconAStr, Material.AMETHYST_SHARD);
        List<String> commandsA = core.getPlugin().getConfig().getStringList(configPath + ".option-a.commands");

        List<String> loreA = new ArrayList<>();
        loreA.add("§8────────────────────");
        loreA.add("§7Phần thưởng: " + nameA);
        loreA.add("§8────────────────────");
        loreA.add("");
        loreA.add("§e➤ Click để chọn!");

        ItemStack optionAItem = createItem(iconA, "§a§l🎁 Phần Thưởng A", loreA);
        inventory.setItem(11, optionAItem);

        // VS indicator (slot 13)
        ItemStack vsItem = createItem(Material.GOLD_INGOT, "§6§lHAY", "§7Chọn 1 trong 2 phần thưởng");
        inventory.setItem(13, vsItem);

        // Option B (slot 15)
        String nameB = core.colorize(core.getPlugin().getConfig().getString(configPath + ".option-b.name", "&bPhần thưởng B"));
        String iconBStr = core.getPlugin().getConfig().getString(configPath + ".option-b.icon", "GOLD_INGOT");
        Material iconB = parseMaterial(iconBStr, Material.GOLD_INGOT);
        List<String> commandsB = core.getPlugin().getConfig().getStringList(configPath + ".option-b.commands");

        List<String> loreB = new ArrayList<>();
        loreB.add("§8────────────────────");
        loreB.add("§7Phần thưởng: " + nameB);
        loreB.add("§8────────────────────");
        loreB.add("");
        loreB.add("§e➤ Click để chọn!");

        ItemStack optionBItem = createItem(iconB, "§b§l🎁 Phần Thưởng B", loreB);
        inventory.setItem(15, optionBItem);

        // Back button (slot 22)
        ItemStack backBtn = createItem(Material.ARROW,
                "§e◀ Quay Lại",
                "§7Quay lại danh sách phần thưởng");
        inventory.setItem(22, backBtn);
    }

    private Material parseMaterial(String name, Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public static void handleClick(TurtleGopyCore core, InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RewardChooseGUI gui)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        switch (slot) {
            case 11 -> {
                // Option A
                claimReward(core, player, gui.rewardId, gui.rewardType, "option-a");
            }
            case 15 -> {
                // Option B
                claimReward(core, player, gui.rewardId, gui.rewardType, "option-b");
            }
            case 22 -> {
                // Back
                player.closeInventory();
                RewardListGUI.open(core, player, 0);
            }
        }
    }

    private static void claimReward(TurtleGopyCore core, Player player, UUID rewardId,
                                     RewardListGUI.RewardType type, String option) {
        player.closeInventory();

        if (type == RewardListGUI.RewardType.FEEDBACK) {
            core.getFeedbackManager().claimReward(rewardId, option);
        } else {
            core.getBugReportManager().claimReward(rewardId, option);
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
