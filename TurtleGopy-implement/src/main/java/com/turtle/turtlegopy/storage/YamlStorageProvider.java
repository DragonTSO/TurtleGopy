package com.turtle.turtlegopy.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.turtle.turtlegopy.api.model.Feedback;
import com.turtle.turtlegopy.api.model.FeedbackStatus;
import com.turtle.turtlegopy.api.storage.StorageProvider;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class YamlStorageProvider implements StorageProvider {

    private final TurtleGopyCore core;
    private File dataFile;
    private YamlConfiguration dataConfig;

    public YamlStorageProvider(TurtleGopyCore core) {
        this.core = core;
    }

    @Override
    public void init() {
        File dataFolder = new File(core.getPlugin().getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        dataFile = new File(dataFolder, "feedbacks.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                core.getPlugin().getLogger().severe("Không thể tạo file feedbacks.yml: " + e.getMessage());
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        core.getPlugin().getLogger().info("Đã tải " + getAll().size() + " góp ý từ YAML.");
    }

    @Override
    public void shutdown() {
        saveData();
    }

    @Override
    public void save(Feedback feedback) {
        String path = "feedbacks." + feedback.getId().toString();
        dataConfig.set(path + ".player-uuid", feedback.getPlayerUUID().toString());
        dataConfig.set(path + ".player-name", feedback.getPlayerName());
        dataConfig.set(path + ".content", feedback.getContent());
        dataConfig.set(path + ".status", feedback.getStatus().name());
        dataConfig.set(path + ".created-at", feedback.getCreatedAt());
        dataConfig.set(path + ".admin-note", feedback.getAdminNote());
        dataConfig.set(path + ".reward-given", feedback.isRewardGiven());
        saveData();
    }

    @Override
    public void update(Feedback feedback) {
        save(feedback);
    }

    @Override
    public void delete(UUID feedbackId) {
        dataConfig.set("feedbacks." + feedbackId.toString(), null);
        saveData();
    }

    @Override
    public Feedback getById(UUID feedbackId) {
        String path = "feedbacks." + feedbackId.toString();
        if (!dataConfig.contains(path)) return null;

        return loadFeedback(feedbackId.toString(), dataConfig.getConfigurationSection(path));
    }

    @Override
    public List<Feedback> getByPlayer(UUID playerUUID) {
        List<Feedback> result = new ArrayList<>();
        ConfigurationSection section = dataConfig.getConfigurationSection("feedbacks");
        if (section == null) return result;

        for (String key : section.getKeys(false)) {
            ConfigurationSection feedbackSection = section.getConfigurationSection(key);
            if (feedbackSection == null) continue;

            String storedUUID = feedbackSection.getString("player-uuid", "");
            if (storedUUID.equals(playerUUID.toString())) {
                Feedback feedback = loadFeedback(key, feedbackSection);
                if (feedback != null) {
                    result.add(feedback);
                }
            }
        }

        return result;
    }

    @Override
    public List<Feedback> getAll() {
        List<Feedback> result = new ArrayList<>();
        ConfigurationSection section = dataConfig.getConfigurationSection("feedbacks");
        if (section == null) return result;

        for (String key : section.getKeys(false)) {
            ConfigurationSection feedbackSection = section.getConfigurationSection(key);
            if (feedbackSection == null) continue;

            Feedback feedback = loadFeedback(key, feedbackSection);
            if (feedback != null) {
                result.add(feedback);
            }
        }

        return result;
    }

    private Feedback loadFeedback(String id, ConfigurationSection section) {
        try {
            return Feedback.builder()
                    .id(UUID.fromString(id))
                    .playerUUID(UUID.fromString(section.getString("player-uuid", "")))
                    .playerName(section.getString("player-name", "Unknown"))
                    .content(section.getString("content", ""))
                    .status(FeedbackStatus.valueOf(section.getString("status", "PENDING")))
                    .createdAt(section.getLong("created-at", System.currentTimeMillis()))
                    .adminNote(section.getString("admin-note", ""))
                    .rewardGiven(section.getBoolean("reward-given", false))
                    .build();
        } catch (Exception e) {
            core.getPlugin().getLogger().warning("Lỗi khi tải feedback " + id + ": " + e.getMessage());
            return null;
        }
    }

    private void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            core.getPlugin().getLogger().severe("Không thể lưu feedbacks.yml: " + e.getMessage());
        }
    }
}
