package com.courier.service;

import com.courier.database.ShipmentDAO;
import com.courier.database.StatusLogDAO;
import com.courier.model.Shipment;
import com.courier.model.ShipmentStatus;
import com.courier.model.StatusLog;
import com.courier.util.TrackingNumberGenerator;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Business logic layer for shipment operations.
 */
public class ShipmentService {

    private final ShipmentDAO shipmentDAO;
    private final StatusLogDAO statusLogDAO;

    public ShipmentService() {
        this.shipmentDAO  = new ShipmentDAO();
        this.statusLogDAO = new StatusLogDAO();
    }

    /**
     * Validates and registers a new shipment dispatch, generates a tracking number,
     * and logs the initial DISPATCHED event.
     *
     * @throws IllegalArgumentException if required fields are missing or invalid.
     */
    public Shipment dispatchShipment(Shipment shipment) throws SQLException {
        // ── Validate required sender fields ──────────────────────────────────
        if (shipment.getSender() == null)
            throw new IllegalArgumentException("Sender information is required.");
        if (isBlank(shipment.getSender().getName()))
            throw new IllegalArgumentException("Sender name is required.");
        if (isBlank(shipment.getSender().getPhone()))
            throw new IllegalArgumentException("Sender phone is required.");
        if (isBlank(shipment.getSender().getAddress()))
            throw new IllegalArgumentException("Sender address is required.");

        // ── Validate required receiver fields ─────────────────────────────────
        if (shipment.getReceiver() == null)
            throw new IllegalArgumentException("Receiver information is required.");
        if (isBlank(shipment.getReceiver().getName()))
            throw new IllegalArgumentException("Receiver name is required.");
        if (isBlank(shipment.getReceiver().getPhone()))
            throw new IllegalArgumentException("Receiver phone is required.");
        if (isBlank(shipment.getReceiver().getAddress()))
            throw new IllegalArgumentException("Receiver address is required.");

        // ── Validate parcel details ───────────────────────────────────────────
        if (isBlank(shipment.getParcelDescription()))
            throw new IllegalArgumentException("Parcel description is required.");
        if (shipment.getWeightKg() <= 0)
            throw new IllegalArgumentException("Weight must be greater than 0.");
        if (shipment.getDeclaredValue() < 0)
            throw new IllegalArgumentException("Declared value cannot be negative.");

        // ── Set defaults for optional fields ─────────────────────────────────
        if (isBlank(shipment.getServiceType()))
            shipment.setServiceType("Standard");
        if (isBlank(shipment.getParcelCategory()))
            shipment.setParcelCategory("Other");

        shipment.setTrackingNumber(TrackingNumberGenerator.generate());
        shipment.setStatus(ShipmentStatus.DISPATCHED);
        shipment.setDispatchDate(LocalDateTime.now());
        shipment.setLastUpdated(LocalDateTime.now());

        LocalDateTime eta = switch (shipment.getServiceType()) {
            case "Express"  -> LocalDateTime.now().plusDays(2);
            case "Same-Day" -> LocalDateTime.now().plusHours(8);
            default         -> LocalDateTime.now().plusDays(5);
        };
        shipment.setEstimatedDelivery(eta);

        int id = shipmentDAO.insertShipment(shipment);
        if (id == -1)
            throw new SQLException("Failed to persist shipment — no generated key returned.");
        shipment.setId(id);

        statusLogDAO.insertLog(new StatusLog(
            shipment.getTrackingNumber(),
            ShipmentStatus.DISPATCHED,
            isBlank(shipment.getCurrentLocation()) ? "Origin Hub" : shipment.getCurrentLocation(),
            "Shipment dispatched successfully.",
            "System"
        ));
        return shipment;
    }

    /**
     * Updates the status of an existing shipment and adds an audit log entry.
     *
     * @throws IllegalArgumentException if the tracking number is blank or required params are missing.
     * @throws IllegalStateException    if the transition is not allowed (e.g., DELIVERED -> non-RETURNED).
     */
    public boolean updateShipmentStatus(String trackingNumber, ShipmentStatus newStatus,
                                        String location, String remarks, String updatedBy)
            throws SQLException {
        if (isBlank(trackingNumber))
            throw new IllegalArgumentException("Tracking number cannot be empty.");
        if (newStatus == null)
            throw new IllegalArgumentException("New status must be specified.");

        // Provide safe defaults for optional free-text fields
        String safeLocation  = isBlank(location)  ? "N/A"    : location.trim();
        String safeRemarks   = isBlank(remarks)   ? ""       : remarks.trim();
        String safeUpdatedBy = isBlank(updatedBy) ? "System" : updatedBy.trim();

        Shipment existing = shipmentDAO.findByTrackingNumber(trackingNumber);
        if (existing == null) return false;

        if (existing.getStatus() == ShipmentStatus.DELIVERED
                && newStatus != ShipmentStatus.RETURNED) {
            throw new IllegalStateException("Shipment already DELIVERED. Only RETURNED is allowed next.");
        }

        if (existing.getStatus() == ShipmentStatus.RETURNED) {
            throw new IllegalStateException("Shipment has been RETURNED and cannot be updated further.");
        }

        boolean updated = shipmentDAO.updateStatus(trackingNumber, newStatus, safeLocation, safeRemarks);
        if (updated) {
            statusLogDAO.insertLog(new StatusLog(
                trackingNumber, newStatus, safeLocation, safeRemarks, safeUpdatedBy));
        }
        return updated;
    }

    // ── Lookup helpers ──────────────────────────────────────────────────────────

    public Shipment trackShipment(String tn) throws SQLException {
        if (isBlank(tn)) throw new IllegalArgumentException("Tracking number cannot be empty.");
        return shipmentDAO.findByTrackingNumber(tn.trim().toUpperCase());
    }

    public List<StatusLog> getShipmentHistory(String tn) throws SQLException {
        if (isBlank(tn)) throw new IllegalArgumentException("Tracking number cannot be empty.");
        return statusLogDAO.findByTrackingNumber(tn.trim().toUpperCase());
    }

    public List<Shipment> getAllShipments() throws SQLException {
        return shipmentDAO.findAll();
    }

    public List<Shipment> getShipmentsByStatus(ShipmentStatus s) throws SQLException {
        return shipmentDAO.findByStatus(s);
    }

    public List<Shipment> searchBySender(String name) throws SQLException {
        if (isBlank(name)) throw new IllegalArgumentException("Search term cannot be empty.");
        return shipmentDAO.findBySenderName(name.trim());
    }

    public List<Shipment> searchByReceiver(String name) throws SQLException {
        if (isBlank(name)) throw new IllegalArgumentException("Search term cannot be empty.");
        return shipmentDAO.findByReceiverName(name.trim());
    }

    public List<StatusLog> getRecentActivity(int limit) throws SQLException {
        return statusLogDAO.findRecentLogs(limit);
    }

    public boolean deleteShipment(String tn) throws SQLException {
        if (isBlank(tn)) throw new IllegalArgumentException("Tracking number cannot be empty.");
        return shipmentDAO.deleteShipment(tn.trim().toUpperCase());
    }

    public int getTotalCount() throws SQLException {
        return shipmentDAO.countAll();
    }

    public int getCountByStatus(ShipmentStatus s) throws SQLException {
        return shipmentDAO.countByStatus(s);
    }

    // ── Internal helpers ────────────────────────────────────────────────────────

    /** Returns true if the string is null or contains only whitespace. */
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
