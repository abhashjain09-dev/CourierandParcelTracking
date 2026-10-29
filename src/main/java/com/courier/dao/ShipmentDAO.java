package com.courier.dao;

import com.courier.database.DatabaseManager;
import com.courier.model.Person;
import com.courier.model.Shipment;
import com.courier.model.ShipmentStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Shipment entity.
 * Executes parameterized SQL operations using PreparedStatements.
 */
public class ShipmentDAO {

    private final DatabaseManager dbManager;

    public ShipmentDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public int insertShipment(Shipment s) throws SQLException {
        String sql =
            "INSERT INTO shipments (" +
            "  tracking_number, status, service_type, parcel_description, parcel_category," +
            "  weight_kg, declared_value, is_fragile, current_location, remarks," +
            "  dispatch_date, estimated_delivery, last_updated," +
            "  sender_name, sender_address, sender_city, sender_state, sender_pincode, sender_phone, sender_email," +
            "  receiver_name, receiver_address, receiver_city, receiver_state, receiver_pincode, receiver_phone, receiver_email" +
            ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = dbManager.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1,  s.getTrackingNumber());
            ps.setString(2,  s.getStatus().name());
            ps.setString(3,  s.getServiceType());
            ps.setString(4,  s.getParcelDescription());
            ps.setString(5,  s.getParcelCategory());
            ps.setDouble(6,  s.getWeightKg());
            ps.setDouble(7,  s.getDeclaredValue());
            ps.setInt(8,     s.isFragile() ? 1 : 0);
            ps.setString(9,  s.getCurrentLocation());
            ps.setString(10, s.getRemarks());
            ps.setString(11, s.getDispatchDate() != null ? s.getDispatchDate().toString() : LocalDateTime.now().toString());
            ps.setString(12, s.getEstimatedDelivery() != null ? s.getEstimatedDelivery().toString() : null);
            ps.setString(13, s.getLastUpdated() != null ? s.getLastUpdated().toString() : LocalDateTime.now().toString());
            ps.setString(14, s.getSender().getName());
            ps.setString(15, s.getSender().getAddress());
            ps.setString(16, s.getSender().getCity());
            ps.setString(17, s.getSender().getState());
            ps.setString(18, s.getSender().getPincode());
            ps.setString(19, s.getSender().getPhone());
            ps.setString(20, s.getSender().getEmail());
            ps.setString(21, s.getReceiver().getName());
            ps.setString(22, s.getReceiver().getAddress());
            ps.setString(23, s.getReceiver().getCity());
            ps.setString(24, s.getReceiver().getState());
            ps.setString(25, s.getReceiver().getPincode());
            ps.setString(26, s.getReceiver().getPhone());
            ps.setString(27, s.getReceiver().getEmail());

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public boolean updateShipment(Shipment s) throws SQLException {
        String sql =
            "UPDATE shipments SET " +
            "  service_type=?, parcel_description=?, parcel_category=?, weight_kg=?, declared_value=?," +
            "  is_fragile=?, current_location=?, remarks=?, estimated_delivery=?, last_updated=?," +
            "  sender_name=?, sender_address=?, sender_city=?, sender_state=?, sender_pincode=?, sender_phone=?, sender_email=?," +
            "  receiver_name=?, receiver_address=?, receiver_city=?, receiver_state=?, receiver_pincode=?, receiver_phone=?, receiver_email=? " +
            "WHERE tracking_number=?";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1,  s.getServiceType());
            ps.setString(2,  s.getParcelDescription());
            ps.setString(3,  s.getParcelCategory());
            ps.setDouble(4,  s.getWeightKg());
            ps.setDouble(5,  s.getDeclaredValue());
            ps.setInt(6,     s.isFragile() ? 1 : 0);
            ps.setString(7,  s.getCurrentLocation());
            ps.setString(8,  s.getRemarks());
            ps.setString(9,  s.getEstimatedDelivery() != null ? s.getEstimatedDelivery().toString() : null);
            ps.setString(10, LocalDateTime.now().toString());
            ps.setString(11, s.getSender().getName());
            ps.setString(12, s.getSender().getAddress());
            ps.setString(13, s.getSender().getCity());
            ps.setString(14, s.getSender().getState());
            ps.setString(15, s.getSender().getPincode());
            ps.setString(16, s.getSender().getPhone());
            ps.setString(17, s.getSender().getEmail());
            ps.setString(18, s.getReceiver().getName());
            ps.setString(19, s.getReceiver().getAddress());
            ps.setString(20, s.getReceiver().getCity());
            ps.setString(21, s.getReceiver().getState());
            ps.setString(22, s.getReceiver().getPincode());
            ps.setString(23, s.getReceiver().getPhone());
            ps.setString(24, s.getReceiver().getEmail());
            ps.setString(25, s.getTrackingNumber());

            return ps.executeUpdate() > 0;
        }
    }

    public Shipment findByTrackingNumber(String tn) throws SQLException {
        String sql = "SELECT * FROM shipments WHERE UPPER(tracking_number) = UPPER(?)";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, tn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Shipment> findAll() throws SQLException {
        List<Shipment> list = new ArrayList<>();
        String sql = "SELECT * FROM shipments ORDER BY dispatch_date DESC";
        try (Statement st = dbManager.getConnection().createStatement();
             ResultSet rs  = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Shipment> findRecentShipments(int limit) throws SQLException {
        List<Shipment> list = new ArrayList<>();
        String sql = "SELECT * FROM shipments ORDER BY dispatch_date DESC LIMIT ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public List<Shipment> findByStatus(ShipmentStatus status) throws SQLException {
        List<Shipment> list = new ArrayList<>();
        String sql = "SELECT * FROM shipments WHERE status = ? ORDER BY dispatch_date DESC";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public List<Shipment> search(String query) throws SQLException {
        List<Shipment> list = new ArrayList<>();
        String pattern = "%" + query.trim() + "%";
        String sql =
            "SELECT * FROM shipments WHERE " +
            "  LOWER(tracking_number)    LIKE LOWER(?) OR " +
            "  LOWER(sender_name)        LIKE LOWER(?) OR " +
            "  LOWER(receiver_name)      LIKE LOWER(?) OR " +
            "  LOWER(sender_city)        LIKE LOWER(?) OR " +
            "  LOWER(receiver_city)      LIKE LOWER(?) OR " +
            "  LOWER(parcel_description) LIKE LOWER(?) OR " +
            "  LOWER(current_location)   LIKE LOWER(?) " +
            "ORDER BY dispatch_date DESC";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            for (int i = 1; i <= 7; i++) {
                ps.setString(i, pattern);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public List<Shipment> findBySenderName(String name) throws SQLException {
        List<Shipment> list = new ArrayList<>();
        String sql = "SELECT * FROM shipments WHERE LOWER(sender_name) LIKE LOWER(?) ORDER BY dispatch_date DESC";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, "%" + name + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public List<Shipment> findByReceiverName(String name) throws SQLException {
        List<Shipment> list = new ArrayList<>();
        String sql = "SELECT * FROM shipments WHERE LOWER(receiver_name) LIKE LOWER(?) ORDER BY dispatch_date DESC";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, "%" + name + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public boolean updateStatus(String tn, ShipmentStatus status,
                                String location, String remarks) throws SQLException {
        String sql = "UPDATE shipments SET status=?, current_location=?, remarks=?, last_updated=? WHERE UPPER(tracking_number)=UPPER(?)";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, location);
            ps.setString(3, remarks);
            ps.setString(4, LocalDateTime.now().toString());
            ps.setString(5, tn);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteShipment(String tn) throws SQLException {
        String sql1 = "DELETE FROM status_logs WHERE UPPER(tracking_number) = UPPER(?)";
        String sql2 = "DELETE FROM shipments   WHERE UPPER(tracking_number) = UPPER(?)";
        Connection conn = dbManager.getConnection();
        boolean oldAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(sql1);
                 PreparedStatement ps2 = conn.prepareStatement(sql2)) {
                ps1.setString(1, tn);
                ps1.executeUpdate();

                ps2.setString(1, tn);
                int affected = ps2.executeUpdate();
                conn.commit();
                return affected > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    public int countByStatus(ShipmentStatus status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM shipments WHERE status = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM shipments";
        try (Statement st = dbManager.getConnection().createStatement();
             ResultSet rs  = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private Shipment mapRow(ResultSet rs) throws SQLException {
        Shipment s = new Shipment();
        s.setId(rs.getInt("id"));
        s.setTrackingNumber(rs.getString("tracking_number"));
        s.setStatus(ShipmentStatus.fromString(rs.getString("status")));
        s.setServiceType(rs.getString("service_type"));
        s.setParcelDescription(rs.getString("parcel_description"));
        s.setParcelCategory(rs.getString("parcel_category"));
        s.setWeightKg(rs.getDouble("weight_kg"));
        s.setDeclaredValue(rs.getDouble("declared_value"));
        s.setFragile(rs.getInt("is_fragile") == 1);
        s.setCurrentLocation(rs.getString("current_location"));
        s.setRemarks(rs.getString("remarks"));

        String d = rs.getString("dispatch_date");
        if (d != null && !d.isEmpty()) s.setDispatchDate(LocalDateTime.parse(d));

        String e = rs.getString("estimated_delivery");
        if (e != null && !e.isEmpty()) s.setEstimatedDelivery(LocalDateTime.parse(e));

        String u = rs.getString("last_updated");
        if (u != null && !u.isEmpty()) s.setLastUpdated(LocalDateTime.parse(u));

        Person snd = new Person();
        snd.setName(rs.getString("sender_name"));
        snd.setAddress(rs.getString("sender_address"));
        snd.setCity(rs.getString("sender_city"));
        snd.setState(rs.getString("sender_state"));
        snd.setPincode(rs.getString("sender_pincode"));
        snd.setPhone(rs.getString("sender_phone"));
        snd.setEmail(rs.getString("sender_email"));
        s.setSender(snd);

        Person rcv = new Person();
        rcv.setName(rs.getString("receiver_name"));
        rcv.setAddress(rs.getString("receiver_address"));
        rcv.setCity(rs.getString("receiver_city"));
        rcv.setState(rs.getString("receiver_state"));
        rcv.setPincode(rs.getString("receiver_pincode"));
        rcv.setPhone(rs.getString("receiver_phone"));
        rcv.setEmail(rs.getString("receiver_email"));
        s.setReceiver(rcv);

        return s;
    }
}
