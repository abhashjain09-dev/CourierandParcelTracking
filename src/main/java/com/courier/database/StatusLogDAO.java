package com.courier.database;

import com.courier.model.ShipmentStatus;
import com.courier.model.StatusLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for StatusLog entries.
 */
public class StatusLogDAO {

    private final DatabaseManager dbManager;

    public StatusLogDAO() { this.dbManager = DatabaseManager.getInstance(); }

    public int insertLog(StatusLog log) throws SQLException {
        String sql = "INSERT INTO status_logs (tracking_number, status, location, remarks, updated_by, timestamp) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = dbManager.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, log.getTrackingNumber());
            ps.setString(2, log.getStatus().name());
            ps.setString(3, log.getLocation());
            ps.setString(4, log.getRemarks());
            ps.setString(5, log.getUpdatedBy());
            ps.setString(6, log.getTimestamp().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    public List<StatusLog> findByTrackingNumber(String tn) throws SQLException {
        List<StatusLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM status_logs WHERE tracking_number = ? ORDER BY timestamp ASC";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, tn);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) logs.add(mapRow(rs));
            }
        }
        return logs;
    }

    public List<StatusLog> findRecentLogs(int limit) throws SQLException {
        List<StatusLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM status_logs ORDER BY timestamp DESC LIMIT ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) logs.add(mapRow(rs));
            }
        }
        return logs;
    }

    private StatusLog mapRow(ResultSet rs) throws SQLException {
        StatusLog log = new StatusLog();
        log.setId(rs.getInt("id"));
        log.setTrackingNumber(rs.getString("tracking_number"));
        log.setStatus(ShipmentStatus.fromString(rs.getString("status")));
        log.setLocation(rs.getString("location"));
        log.setRemarks(rs.getString("remarks"));
        log.setUpdatedBy(rs.getString("updated_by"));
        String ts = rs.getString("timestamp");
        if (ts != null) log.setTimestamp(LocalDateTime.parse(ts));
        return log;
    }
}
