package com.turtle.turtlegopy.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.turtle.turtlegopy.api.model.Feedback;
import com.turtle.turtlegopy.api.model.FeedbackStatus;
import com.turtle.turtlegopy.api.storage.StorageProvider;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.Getter;

@Getter
public class DatabaseStorageProvider implements StorageProvider {

    private final TurtleGopyCore core;
    private HikariDataSource dataSource;
    private String tablePrefix;

    public DatabaseStorageProvider(TurtleGopyCore core) {
        this.core = core;
    }

    @Override
    public void init() {
        tablePrefix = core.getPlugin().getConfig().getString("storage.database.table-prefix", "turtlegopy_");
        String dbType = core.getPlugin().getConfig().getString("storage.database.type", "SQLITE").toUpperCase();

        HikariConfig config = new HikariConfig();

        if ("MYSQL".equals(dbType)) {
            String host = core.getPlugin().getConfig().getString("storage.database.host", "localhost");
            int port = core.getPlugin().getConfig().getInt("storage.database.port", 3306);
            String dbName = core.getPlugin().getConfig().getString("storage.database.name", "turtlegopy");
            String username = core.getPlugin().getConfig().getString("storage.database.username", "root");
            String password = core.getPlugin().getConfig().getString("storage.database.password", "");

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&autoReconnect=true&useUnicode=true&characterEncoding=UTF-8");
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            String dbPath = core.getPlugin().getDataFolder().getAbsolutePath() + "/data/turtlegopy.db";
            config.setJdbcUrl("jdbc:sqlite:" + dbPath);
            config.setDriverClassName("org.sqlite.JDBC");
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(10000);
        config.setMaxLifetime(600000);
        config.setPoolName("TurtleGopy-Pool");

        try {
            dataSource = new HikariDataSource(config);
            createTable();
            core.getPlugin().getLogger().info("Đã kết nối database (" + dbType + ") thành công!");
        } catch (Exception e) {
            core.getPlugin().getLogger().severe("Lỗi kết nối database: " + e.getMessage());
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "feedbacks (" +
                "id VARCHAR(36) PRIMARY KEY, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "player_name VARCHAR(32) NOT NULL, " +
                "content TEXT NOT NULL, " +
                "status VARCHAR(20) NOT NULL DEFAULT 'PENDING', " +
                "created_at BIGINT NOT NULL, " +
                "admin_note TEXT DEFAULT '', " +
                "reward_given BOOLEAN DEFAULT FALSE, " +
                "reward_pending BOOLEAN DEFAULT FALSE" +
                ")";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi tạo bảng: " + e.getMessage());
        }
    }

    @Override
    public void save(Feedback feedback) {
        String sql = "INSERT INTO " + tablePrefix + "feedbacks " +
                "(id, player_uuid, player_name, content, status, created_at, admin_note, reward_given, reward_pending) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, feedback.getId().toString());
            stmt.setString(2, feedback.getPlayerUUID().toString());
            stmt.setString(3, feedback.getPlayerName());
            stmt.setString(4, feedback.getContent());
            stmt.setString(5, feedback.getStatus().name());
            stmt.setLong(6, feedback.getCreatedAt());
            stmt.setString(7, feedback.getAdminNote());
            stmt.setBoolean(8, feedback.isRewardGiven());
            stmt.setBoolean(9, feedback.isRewardPending());
            stmt.executeUpdate();
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi lưu feedback: " + e.getMessage());
        }
    }

    @Override
    public void update(Feedback feedback) {
        String sql = "UPDATE " + tablePrefix + "feedbacks SET " +
                "status = ?, admin_note = ?, reward_given = ?, reward_pending = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, feedback.getStatus().name());
            stmt.setString(2, feedback.getAdminNote());
            stmt.setBoolean(3, feedback.isRewardGiven());
            stmt.setBoolean(4, feedback.isRewardPending());
            stmt.setString(5, feedback.getId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi cập nhật feedback: " + e.getMessage());
        }
    }

    @Override
    public void delete(UUID feedbackId) {
        String sql = "DELETE FROM " + tablePrefix + "feedbacks WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, feedbackId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi xóa feedback: " + e.getMessage());
        }
    }

    @Override
    public Feedback getById(UUID feedbackId) {
        String sql = "SELECT * FROM " + tablePrefix + "feedbacks WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, feedbackId.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi tải feedback: " + e.getMessage());
        }

        return null;
    }

    @Override
    public List<Feedback> getByPlayer(UUID playerUUID) {
        List<Feedback> result = new ArrayList<>();
        String sql = "SELECT * FROM " + tablePrefix + "feedbacks WHERE player_uuid = ? ORDER BY created_at DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi tải feedbacks by player: " + e.getMessage());
        }

        return result;
    }

    @Override
    public List<Feedback> getAll() {
        List<Feedback> result = new ArrayList<>();
        String sql = "SELECT * FROM " + tablePrefix + "feedbacks ORDER BY created_at DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi tải tất cả feedbacks: " + e.getMessage());
        }

        return result;
    }

    private Feedback mapResultSet(ResultSet rs) throws SQLException {
        return Feedback.builder()
                .id(UUID.fromString(rs.getString("id")))
                .playerUUID(UUID.fromString(rs.getString("player_uuid")))
                .playerName(rs.getString("player_name"))
                .content(rs.getString("content"))
                .status(FeedbackStatus.valueOf(rs.getString("status")))
                .createdAt(rs.getLong("created_at"))
                .adminNote(rs.getString("admin_note"))
                .rewardGiven(rs.getBoolean("reward_given"))
                .rewardPending(rs.getBoolean("reward_pending"))
                .build();
    }
}
