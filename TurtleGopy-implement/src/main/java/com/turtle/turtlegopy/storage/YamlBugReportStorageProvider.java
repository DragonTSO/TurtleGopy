package com.turtle.turtlegopy.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.turtle.turtlegopy.api.model.BugReport;
import com.turtle.turtlegopy.api.model.BugReportStatus;
import com.turtle.turtlegopy.api.storage.BugReportStorageProvider;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class YamlBugReportStorageProvider implements BugReportStorageProvider {

    private final TurtleGopyCore core;
    private File dataFile;
    private YamlConfiguration dataConfig;

    public YamlBugReportStorageProvider(TurtleGopyCore core) {
        this.core = core;
    }

    @Override
    public void init() {
        File dataFolder = new File(core.getPlugin().getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        dataFile = new File(dataFolder, "bugreports.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                core.getPlugin().getLogger().severe("Không thể tạo file bugreports.yml: " + e.getMessage());
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        core.getPlugin().getLogger().info("Đã tải " + getAll().size() + " báo lỗi từ YAML.");
    }

    @Override
    public void shutdown() {
        saveData();
    }

    @Override
    public void save(BugReport report) {
        String path = "bugreports." + report.getId().toString();
        dataConfig.set(path + ".player-uuid", report.getPlayerUUID().toString());
        dataConfig.set(path + ".player-name", report.getPlayerName());
        dataConfig.set(path + ".content", report.getContent());
        dataConfig.set(path + ".status", report.getStatus().name());
        dataConfig.set(path + ".created-at", report.getCreatedAt());
        dataConfig.set(path + ".admin-note", report.getAdminNote());
        dataConfig.set(path + ".reward-given", report.isRewardGiven());
        dataConfig.set(path + ".reward-pending", report.isRewardPending());
        saveData();
    }

    @Override
    public void update(BugReport report) {
        save(report);
    }

    @Override
    public void delete(UUID reportId) {
        dataConfig.set("bugreports." + reportId.toString(), null);
        saveData();
    }

    @Override
    public BugReport getById(UUID reportId) {
        String path = "bugreports." + reportId.toString();
        if (!dataConfig.contains(path)) return null;

        return loadReport(reportId.toString(), dataConfig.getConfigurationSection(path));
    }

    @Override
    public List<BugReport> getByPlayer(UUID playerUUID) {
        List<BugReport> result = new ArrayList<>();
        ConfigurationSection section = dataConfig.getConfigurationSection("bugreports");
        if (section == null) return result;

        for (String key : section.getKeys(false)) {
            ConfigurationSection reportSection = section.getConfigurationSection(key);
            if (reportSection == null) continue;

            String storedUUID = reportSection.getString("player-uuid", "");
            if (storedUUID.equals(playerUUID.toString())) {
                BugReport report = loadReport(key, reportSection);
                if (report != null) {
                    result.add(report);
                }
            }
        }

        return result;
    }

    @Override
    public List<BugReport> getAll() {
        List<BugReport> result = new ArrayList<>();
        ConfigurationSection section = dataConfig.getConfigurationSection("bugreports");
        if (section == null) return result;

        for (String key : section.getKeys(false)) {
            ConfigurationSection reportSection = section.getConfigurationSection(key);
            if (reportSection == null) continue;

            BugReport report = loadReport(key, reportSection);
            if (report != null) {
                result.add(report);
            }
        }

        return result;
    }

    private BugReport loadReport(String id, ConfigurationSection section) {
        try {
            return BugReport.builder()
                    .id(UUID.fromString(id))
                    .playerUUID(UUID.fromString(section.getString("player-uuid", "")))
                    .playerName(section.getString("player-name", "Unknown"))
                    .content(section.getString("content", ""))
                    .status(BugReportStatus.valueOf(section.getString("status", "PENDING")))
                    .createdAt(section.getLong("created-at", System.currentTimeMillis()))
                    .adminNote(section.getString("admin-note", ""))
                    .rewardGiven(section.getBoolean("reward-given", false))
                    .rewardPending(section.getBoolean("reward-pending", false))
                    .build();
        } catch (Exception e) {
            core.getPlugin().getLogger().warning("Lỗi khi tải bug report " + id + ": " + e.getMessage());
            return null;
        }
    }

    private void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            core.getPlugin().getLogger().severe("Không thể lưu bugreports.yml: " + e.getMessage());
        }
    }
}
