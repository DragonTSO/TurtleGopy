package com.turtle.turtlegopy.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.turtle.turtlegopy.api.model.SupportChatMessage;
import com.turtle.turtlegopy.api.storage.SupportChatStorageProvider;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import com.zaxxer.hikari.HikariDataSource;

public class DatabaseSupportChatStorageProvider implements SupportChatStorageProvider {

    private final TurtleGopyCore core;
    private final HikariDataSource dataSource;
    private final String tablePrefix;

    public DatabaseSupportChatStorageProvider(TurtleGopyCore core, HikariDataSource dataSource) {
        this.core = core;
        this.dataSource = dataSource;
        this.tablePrefix = core.getPlugin().getConfig().getString("storage.database.table-prefix", "turtlegopy_");
    }

    @Override
    public void init() {
        createTable();
        core.getPlugin().getLogger().info("Đã khởi tạo bảng chat hỗ trợ trong database.");
    }

    @Override
    public void shutdown() {
        // DataSource is managed by DatabaseStorageProvider
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "supportchat (" +
                "id VARCHAR(36) PRIMARY KEY, " +
                "ticket_id VARCHAR(36) NOT NULL, " +
                "sender_uuid VARCHAR(36) NOT NULL, " +
                "sender_name VARCHAR(32) NOT NULL, " +
                "message TEXT NOT NULL, " +
                "timestamp BIGINT NOT NULL, " +
                "is_staff BOOLEAN DEFAULT FALSE" +
                ")";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi tạo bảng supportchat: " + e.getMessage());
        }
    }

    @Override
    public void saveMessage(SupportChatMessage message) {
        String sql = "INSERT INTO " + tablePrefix + "supportchat " +
                "(id, ticket_id, sender_uuid, sender_name, message, timestamp, is_staff) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, message.getId().toString());
            stmt.setString(2, message.getTicketId().toString());
            stmt.setString(3, message.getSenderUUID().toString());
            stmt.setString(4, message.getSenderName());
            stmt.setString(5, message.getMessage());
            stmt.setLong(6, message.getTimestamp());
            stmt.setBoolean(7, message.isStaff());
            stmt.executeUpdate();
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi lưu chat message: " + e.getMessage());
        }
    }

    @Override
    public List<SupportChatMessage> getMessages(UUID ticketId) {
        List<SupportChatMessage> result = new ArrayList<>();
        String sql = "SELECT * FROM " + tablePrefix + "supportchat WHERE ticket_id = ? ORDER BY timestamp ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ticketId.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi tải chat messages: " + e.getMessage());
        }

        return result;
    }

    @Override
    public void deleteByTicket(UUID ticketId) {
        String sql = "DELETE FROM " + tablePrefix + "supportchat WHERE ticket_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ticketId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            core.getPlugin().getLogger().severe("Lỗi xóa chat messages: " + e.getMessage());
        }
    }

    private SupportChatMessage mapResultSet(ResultSet rs) throws SQLException {
        return SupportChatMessage.builder()
                .id(UUID.fromString(rs.getString("id")))
                .ticketId(UUID.fromString(rs.getString("ticket_id")))
                .senderUUID(UUID.fromString(rs.getString("sender_uuid")))
                .senderName(rs.getString("sender_name"))
                .message(rs.getString("message"))
                .timestamp(rs.getLong("timestamp"))
                .staff(rs.getBoolean("is_staff"))
                .build();
    }
}
