package com.turtle.turtlegopy.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.turtle.turtlegopy.api.model.SupportTicket;
import com.turtle.turtlegopy.api.model.SupportTicketStatus;
import com.turtle.turtlegopy.api.storage.SupportTicketStorageProvider;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class YamlSupportTicketStorageProvider implements SupportTicketStorageProvider {

    private final TurtleGopyCore core;
    private File dataFile;
    private YamlConfiguration dataConfig;

    public YamlSupportTicketStorageProvider(TurtleGopyCore core) {
        this.core = core;
    }

    @Override
    public void init() {
        File dataFolder = new File(core.getPlugin().getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        dataFile = new File(dataFolder, "supporttickets.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                core.getPlugin().getLogger().severe("Không thể tạo file supporttickets.yml: " + e.getMessage());
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        core.getPlugin().getLogger().info("Đã tải " + getAll().size() + " phiếu hỗ trợ từ YAML.");
    }

    @Override
    public void shutdown() {
        saveData();
    }

    @Override
    public void save(SupportTicket ticket) {
        String path = "tickets." + ticket.getId().toString();
        dataConfig.set(path + ".player-uuid", ticket.getPlayerUUID().toString());
        dataConfig.set(path + ".player-name", ticket.getPlayerName());
        dataConfig.set(path + ".content", ticket.getContent());
        dataConfig.set(path + ".status", ticket.getStatus().name());
        dataConfig.set(path + ".created-at", ticket.getCreatedAt());
        dataConfig.set(path + ".admin-note", ticket.getAdminNote());
        dataConfig.set(path + ".reward-given", ticket.isRewardGiven());
        saveData();
    }

    @Override
    public void update(SupportTicket ticket) {
        save(ticket);
    }

    @Override
    public void delete(UUID ticketId) {
        dataConfig.set("tickets." + ticketId.toString(), null);
        saveData();
    }

    @Override
    public SupportTicket getById(UUID ticketId) {
        String path = "tickets." + ticketId.toString();
        if (!dataConfig.contains(path)) return null;

        return loadTicket(ticketId.toString(), dataConfig.getConfigurationSection(path));
    }

    @Override
    public List<SupportTicket> getByPlayer(UUID playerUUID) {
        List<SupportTicket> result = new ArrayList<>();
        ConfigurationSection section = dataConfig.getConfigurationSection("tickets");
        if (section == null) return result;

        for (String key : section.getKeys(false)) {
            ConfigurationSection ticketSection = section.getConfigurationSection(key);
            if (ticketSection == null) continue;

            String storedUUID = ticketSection.getString("player-uuid", "");
            if (storedUUID.equals(playerUUID.toString())) {
                SupportTicket ticket = loadTicket(key, ticketSection);
                if (ticket != null) {
                    result.add(ticket);
                }
            }
        }

        return result;
    }

    @Override
    public List<SupportTicket> getAll() {
        List<SupportTicket> result = new ArrayList<>();
        ConfigurationSection section = dataConfig.getConfigurationSection("tickets");
        if (section == null) return result;

        for (String key : section.getKeys(false)) {
            ConfigurationSection ticketSection = section.getConfigurationSection(key);
            if (ticketSection == null) continue;

            SupportTicket ticket = loadTicket(key, ticketSection);
            if (ticket != null) {
                result.add(ticket);
            }
        }

        return result;
    }

    private SupportTicket loadTicket(String id, ConfigurationSection section) {
        try {
            return SupportTicket.builder()
                    .id(UUID.fromString(id))
                    .playerUUID(UUID.fromString(section.getString("player-uuid", "")))
                    .playerName(section.getString("player-name", "Unknown"))
                    .content(section.getString("content", ""))
                    .status(SupportTicketStatus.valueOf(section.getString("status", "PENDING")))
                    .createdAt(section.getLong("created-at", System.currentTimeMillis()))
                    .adminNote(section.getString("admin-note", ""))
                    .rewardGiven(section.getBoolean("reward-given", false))
                    .build();
        } catch (Exception e) {
            core.getPlugin().getLogger().warning("Lỗi khi tải phiếu hỗ trợ " + id + ": " + e.getMessage());
            return null;
        }
    }

    private void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            core.getPlugin().getLogger().severe("Không thể lưu supporttickets.yml: " + e.getMessage());
        }
    }
}
