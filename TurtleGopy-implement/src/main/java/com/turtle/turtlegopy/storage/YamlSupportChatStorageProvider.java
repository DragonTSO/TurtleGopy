package com.turtle.turtlegopy.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.turtle.turtlegopy.api.model.SupportChatMessage;
import com.turtle.turtlegopy.api.storage.SupportChatStorageProvider;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class YamlSupportChatStorageProvider implements SupportChatStorageProvider {

    private final TurtleGopyCore core;
    private File dataFile;
    private YamlConfiguration dataConfig;

    public YamlSupportChatStorageProvider(TurtleGopyCore core) {
        this.core = core;
    }

    @Override
    public void init() {
        File dataFolder = new File(core.getPlugin().getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        dataFile = new File(dataFolder, "supportchat.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                core.getPlugin().getLogger().severe("Không thể tạo file supportchat.yml: " + e.getMessage());
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        core.getPlugin().getLogger().info("Đã tải dữ liệu chat hỗ trợ từ YAML.");
    }

    @Override
    public void shutdown() {
        saveData();
    }

    @Override
    public void saveMessage(SupportChatMessage message) {
        String path = "chats." + message.getTicketId().toString() + "." + message.getId().toString();
        dataConfig.set(path + ".sender-uuid", message.getSenderUUID().toString());
        dataConfig.set(path + ".sender-name", message.getSenderName());
        dataConfig.set(path + ".message", message.getMessage());
        dataConfig.set(path + ".timestamp", message.getTimestamp());
        dataConfig.set(path + ".is-staff", message.isStaff());
        saveData();
    }

    @Override
    public List<SupportChatMessage> getMessages(UUID ticketId) {
        List<SupportChatMessage> result = new ArrayList<>();
        String path = "chats." + ticketId.toString();
        ConfigurationSection section = dataConfig.getConfigurationSection(path);
        if (section == null) return result;

        for (String key : section.getKeys(false)) {
            ConfigurationSection msgSection = section.getConfigurationSection(key);
            if (msgSection == null) continue;

            try {
                SupportChatMessage msg = SupportChatMessage.builder()
                        .id(UUID.fromString(key))
                        .ticketId(ticketId)
                        .senderUUID(UUID.fromString(msgSection.getString("sender-uuid", "")))
                        .senderName(msgSection.getString("sender-name", "Unknown"))
                        .message(msgSection.getString("message", ""))
                        .timestamp(msgSection.getLong("timestamp", 0))
                        .staff(msgSection.getBoolean("is-staff", false))
                        .build();
                result.add(msg);
            } catch (Exception e) {
                core.getPlugin().getLogger().warning("Lỗi khi tải chat message " + key + ": " + e.getMessage());
            }
        }

        // Sort by timestamp ascending
        result.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
        return result;
    }

    @Override
    public void deleteByTicket(UUID ticketId) {
        dataConfig.set("chats." + ticketId.toString(), null);
        saveData();
    }

    private void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            core.getPlugin().getLogger().severe("Không thể lưu supportchat.yml: " + e.getMessage());
        }
    }
}
