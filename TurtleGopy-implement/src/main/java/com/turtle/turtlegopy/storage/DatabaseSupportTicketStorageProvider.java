package com.turtle.turtlegopy.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.turtle.turtlegopy.api.model.SupportTicket;
import com.turtle.turtlegopy.api.model.SupportTicketStatus;
import com.turtle.turtlegopy.api.storage.SupportTicketStorageProvider;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import com.zaxxer.hikari.HikariDataSource;

public class DatabaseSupportTicketStorageProvider implements SupportTicketStorageProvider {

    private final TurtleGopyCore core;
    private final HikariDataSource dataSource;
    private final String tablePrefix;

    public DatabaseSupportTicketStorageProvider(TurtleGopyCore core, HikariDataSource dataSource) {
        this.core = core;
        this.dataSource = dataSource;
        this.tablePrefix = core.getPlugin().getConfig().getString("storage.database.table-prefix", "turtlegopy_");
    }

    @Override
    public void init() {
        createTable();
        core.getPlugin().getLogger().info("Đã khởi tạo bảng phiếu hỗ trợ trong database.");
    }

    @Override
    public void shutdown() {
        // DataSource is managed by DatabaseStorageProvider
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "supporttickets (" +
                "id VARCHAR(36) PRIMARY KEY, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "player_name VARCHAR(32) NOT NULL, " +
                "content TEXT NOT NULL, " +
                "status VARCHAR(20) NOT NULL DEFAULT 'PENDING', " +
                "created_at BIGINT NOT NULL, " +
                "admin_note TEXT DEFAULT '', " +
                "reward_given BOOLEAN DEFAULT FALSE" +
                ")";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi tạo bảng supporttickets: " + e.getMessage());
        }
    }

    @Override
    public void save(SupportTicket ticket) {
        String sql = "INSERT INTO " + tablePrefix + "supporttickets " +
                "(id, player_uuid, player_name, content, status, created_at, admin_note, reward_given) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ticket.getId().toString());
            stmt.setString(2, ticket.getPlayerUUID().toString());
            stmt.setString(3, ticket.getPlayerName());
            stmt.setString(4, ticket.getContent());
            stmt.setString(5, ticket.getStatus().name());
            stmt.setLong(6, ticket.getCreatedAt());
            stmt.setString(7, ticket.getAdminNote());
            stmt.setBoolean(8, ticket.isRewardGiven());
            stmt.executeUpdate();
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi lưu phiếu hỗ trợ: " + e.getMessage());
        }
    }

    @Override
    public void update(SupportTicket ticket) {
        String sql = "UPDATE " + tablePrefix + "supporttickets SET " +
                "status = ?, admin_note = ?, reward_given = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ticket.getStatus().name());
            stmt.setString(2, ticket.getAdminNote());
            stmt.setBoolean(3, ticket.isRewardGiven());
            stmt.setString(4, ticket.getId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi cập nhật phiếu hỗ trợ: " + e.getMessage());
        }
    }

    @Override
    public void delete(UUID ticketId) {
        String sql = "DELETE FROM " + tablePrefix + "supporttickets WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ticketId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi xóa phiếu hỗ trợ: " + e.getMessage());
        }
    }

    @Override
    public SupportTicket getById(UUID ticketId) {
        String sql = "SELECT * FROM " + tablePrefix + "supporttickets WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ticketId.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi tải phiếu hỗ trợ: " + e.getMessage());
        }

        return null;
    }

    @Override
    public List<SupportTicket> getByPlayer(UUID playerUUID) {
        List<SupportTicket> result = new ArrayList<>();
        String sql = "SELECT * FROM " + tablePrefix + "supporttickets WHERE player_uuid = ? ORDER BY created_at DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi tải phiếu hỗ trợ by player: " + e.getMessage());
        }

        return result;
    }

    @Override
    public List<SupportTicket> getAll() {
        List<SupportTicket> result = new ArrayList<>();
        String sql = "SELECT * FROM " + tablePrefix + "supporttickets ORDER BY created_at DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi tải tất cả phiếu hỗ trợ: " + e.getMessage());
        }

        return result;
    }

    private SupportTicket mapResultSet(ResultSet rs) throws SQLException {
        return SupportTicket.builder()
                .id(UUID.fromString(rs.getString("id")))
                .playerUUID(UUID.fromString(rs.getString("player_uuid")))
                .playerName(rs.getString("player_name"))
                .content(rs.getString("content"))
                .status(SupportTicketStatus.valueOf(rs.getString("status")))
                .createdAt(rs.getLong("created_at"))
                .adminNote(rs.getString("admin_note"))
                .rewardGiven(rs.getBoolean("reward_given"))
                .build();
    }
}
