package com.turtle.turtlegopy.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.turtle.turtlegopy.api.model.BugReport;
import com.turtle.turtlegopy.api.model.BugReportStatus;
import com.turtle.turtlegopy.api.storage.BugReportStorageProvider;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import com.zaxxer.hikari.HikariDataSource;

public class DatabaseBugReportStorageProvider implements BugReportStorageProvider {

    private final TurtleGopyCore core;
    private final HikariDataSource dataSource;
    private final String tablePrefix;

    public DatabaseBugReportStorageProvider(TurtleGopyCore core, HikariDataSource dataSource) {
        this.core = core;
        this.dataSource = dataSource;
        this.tablePrefix = core.getPlugin().getConfig().getString("storage.database.table-prefix", "turtlegopy_");
    }

    @Override
    public void init() {
        createTable();
        core.getPlugin().getLogger().info("Đã khởi tạo bảng bug reports trong database.");
    }

    @Override
    public void shutdown() {
        // DataSource is managed by DatabaseStorageProvider
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "bugreports (" +
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
            core.getPlugin().getLogger().severe("Lỗi tạo bảng bugreports: " + e.getMessage());
        }
    }

    @Override
    public void save(BugReport report) {
        String sql = "INSERT INTO " + tablePrefix + "bugreports " +
                "(id, player_uuid, player_name, content, status, created_at, admin_note, reward_given, reward_pending) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, report.getId().toString());
            stmt.setString(2, report.getPlayerUUID().toString());
            stmt.setString(3, report.getPlayerName());
            stmt.setString(4, report.getContent());
            stmt.setString(5, report.getStatus().name());
            stmt.setLong(6, report.getCreatedAt());
            stmt.setString(7, report.getAdminNote());
            stmt.setBoolean(8, report.isRewardGiven());
            stmt.setBoolean(9, report.isRewardPending());
            stmt.executeUpdate();
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi lưu bug report: " + e.getMessage());
        }
    }

    @Override
    public void update(BugReport report) {
        String sql = "UPDATE " + tablePrefix + "bugreports SET " +
                "status = ?, admin_note = ?, reward_given = ?, reward_pending = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, report.getStatus().name());
            stmt.setString(2, report.getAdminNote());
            stmt.setBoolean(3, report.isRewardGiven());
            stmt.setBoolean(4, report.isRewardPending());
            stmt.setString(5, report.getId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi cập nhật bug report: " + e.getMessage());
        }
    }

    @Override
    public void delete(UUID reportId) {
        String sql = "DELETE FROM " + tablePrefix + "bugreports WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reportId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi xóa bug report: " + e.getMessage());
        }
    }

    @Override
    public BugReport getById(UUID reportId) {
        String sql = "SELECT * FROM " + tablePrefix + "bugreports WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reportId.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi tải bug report: " + e.getMessage());
        }

        return null;
    }

    @Override
    public List<BugReport> getByPlayer(UUID playerUUID) {
        List<BugReport> result = new ArrayList<>();
        String sql = "SELECT * FROM " + tablePrefix + "bugreports WHERE player_uuid = ? ORDER BY created_at DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi tải bug reports by player: " + e.getMessage());
        }

        return result;
    }

    @Override
    public List<BugReport> getAll() {
        List<BugReport> result = new ArrayList<>();
        String sql = "SELECT * FROM " + tablePrefix + "bugreports ORDER BY created_at DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi tải tất cả bug reports: " + e.getMessage());
        }

        return result;
    }

    private BugReport mapResultSet(ResultSet rs) throws SQLException {
        return BugReport.builder()
                .id(UUID.fromString(rs.getString("id")))
                .playerUUID(UUID.fromString(rs.getString("player_uuid")))
                .playerName(rs.getString("player_name"))
                .content(rs.getString("content"))
                .status(BugReportStatus.valueOf(rs.getString("status")))
                .createdAt(rs.getLong("created_at"))
                .adminNote(rs.getString("admin_note"))
                .rewardGiven(rs.getBoolean("reward_given"))
                .rewardPending(rs.getBoolean("reward_pending"))
                .build();
    }
}
