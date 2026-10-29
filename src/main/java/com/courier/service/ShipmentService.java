package com.courier.service;

import com.courier.dao.ShipmentDAO;
import com.courier.dao.StatusLogDAO;
import com.courier.model.Shipment;
import com.courier.model.ShipmentStatus;
import com.courier.model.StatusLog;
import com.courier.util.TrackingNumberGenerator;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Business logic layer for shipment processing, validation,
 * status transitions, ETA calculations, and audit logging.
 */
public class ShipmentService {

    private final ShipmentDAO shipmentDAO;
    private final StatusLogDAO statusLogDAO;

    public ShipmentService() {
        this.shipmentDAO  = new ShipmentDAO();
        this.statusLogDAO = new StatusLogDAO();
    }

    /**
     * Overloaded constructor for unit testing with mocked/custom DAOs.
     */
    public ShipmentService(ShipmentDAO shipmentDAO, StatusLogDAO statusLogDAO) {
        this.shipmentDAO = shipmentDAO;
        this.statusLogDAO = statusLogDAO;
    }

    /**
     * Validates and registers a new shipment dispatch, generates a unique tracking number,
     * calculates estimated delivery date, and logs the initial DISPATCHED event.
     *
     * @throws IllegalArgumentException if required fields are missing or invalid.
     */
    public Shipment dispatchShipment(Shipment shipment) throws SQLException {
        validateShipmentInput(shipment);

        if (isBlank(shipment.getServiceType())) {
            shipment.setServiceType("Standard");
        }
        if (isBlank(shipment.getParcelCategory())) {
            shipment.setParcelCategory("Other");
        }
        if (isBlank(shipment.getTrackingNumber())) {
            shipment.setTrackingNumber(TrackingNumberGenerator.generate());
        }

        shipment.setStatus(ShipmentStatus.DISPATCHED);
        shipment.setDispatchDate(LocalDateTime.now());
        shipment.setLastUpdated(LocalDateTime.now());

        if (shipment.getEstimatedDelivery() == null) {
            shipment.setEstimatedDelivery(calculateEstimatedDelivery(shipment.getServiceType()));
        }

        int id = shipmentDAO.insertShipment(shipment);
        if (id == -1) {
            throw new SQLException("Failed to persist shipment — no database ID returned.");
        }
        shipment.setId(id);

        String initialLocation = isBlank(shipment.getCurrentLocation()) ? "Origin Hub" : shipment.getCurrentLocation().trim();
        statusLogDAO.insertLog(new StatusLog(
            shipment.getTrackingNumber(),
            ShipmentStatus.DISPATCHED,
            initialLocation,
            "Shipment created and dispatched at origin facility.",
            "System"
        ));

        return shipment;
    }

    /**
     * Updates an existing shipment's details (parcel description, weight, receiver address, etc.).
     */
    public boolean updateShipmentDetails(Shipment shipment) throws SQLException {
        if (shipment == null) {
            throw new IllegalArgumentException("Shipment cannot be null.");
        }
        if (isBlank(shipment.getTrackingNumber())) {
            throw new IllegalArgumentException("Tracking number is required for update.");
        }
        validateShipmentInput(shipment);

        Shipment existing = shipmentDAO.findByTrackingNumber(shipment.getTrackingNumber());
        if (existing == null) {
            return false;
        }

        boolean updated = shipmentDAO.updateShipment(shipment);
        if (updated) {
            statusLogDAO.insertLog(new StatusLog(
                shipment.getTrackingNumber(),
                existing.getStatus(),
                isBlank(shipment.getCurrentLocation()) ? existing.getCurrentLocation() : shipment.getCurrentLocation(),
                "Shipment record details updated.",
                "Admin"
            ));
        }
        return updated;
    }

    /**
     * Updates the status of an existing shipment and records an audit log entry.
     */
    public boolean updateShipmentStatus(String trackingNumber, ShipmentStatus newStatus,
                                        String location, String remarks, String updatedBy)
            throws SQLException {
        if (isBlank(trackingNumber)) {
            throw new IllegalArgumentException("Tracking number cannot be empty.");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("New status must be specified.");
        }

        String safeLocation  = isBlank(location)  ? "In Transit" : location.trim();
        String safeRemarks   = isBlank(remarks)   ? "Status updated to " + newStatus.getDisplayName() : remarks.trim();
        String safeUpdatedBy = isBlank(updatedBy) ? "Staff" : updatedBy.trim();

        Shipment existing = shipmentDAO.findByTrackingNumber(trackingNumber.trim().toUpperCase());
        if (existing == null) {
            return false;
        }

        // Validate state transitions
        if (existing.getStatus() == ShipmentStatus.DELIVERED && newStatus != ShipmentStatus.RETURNED) {
            throw new IllegalStateException("Shipment is already DELIVERED. Only transition to 'RETURNED' is allowed.");
        }
        if (existing.getStatus() == ShipmentStatus.RETURNED) {
            throw new IllegalStateException("Shipment has been marked RETURNED and cannot be updated further.");
        }

        boolean updated = shipmentDAO.updateStatus(trackingNumber.trim().toUpperCase(), newStatus, safeLocation, safeRemarks);
        if (updated) {
            statusLogDAO.insertLog(new StatusLog(
                trackingNumber.trim().toUpperCase(), newStatus, safeLocation, safeRemarks, safeUpdatedBy));
        }
        return updated;
    }

    /**
     * Calculates the estimated delivery date based on service type.
     */
    public LocalDateTime calculateEstimatedDelivery(String serviceType) {
        if (serviceType == null) return LocalDateTime.now().plusDays(5);
        String s = serviceType.toLowerCase();
        if (s.contains("same-day")) {
            return LocalDateTime.now().plusHours(8);
        } else if (s.contains("overnight")) {
            return LocalDateTime.now().plusDays(1);
        } else if (s.contains("express")) {
            return LocalDateTime.now().plusDays(2);
        } else if (s.contains("international")) {
            return LocalDateTime.now().plusDays(7);
        } else {
            return LocalDateTime.now().plusDays(5);
        }
    }

    // ── Input Validation ────────────────────────────────────────────────────────

    private void validateShipmentInput(Shipment shipment) {
        if (shipment == null) {
            throw new IllegalArgumentException("Shipment data cannot be empty.");
        }

        // Validate Sender
        if (shipment.getSender() == null) {
            throw new IllegalArgumentException("Sender information is required.");
        }
        if (isBlank(shipment.getSender().getName())) {
            throw new IllegalArgumentException("Sender name cannot be empty.");
        }
        if (isBlank(shipment.getSender().getPhone())) {
            throw new IllegalArgumentException("Sender phone number is required.");
        }
        if (isBlank(shipment.getSender().getAddress())) {
            throw new IllegalArgumentException("Sender address is required.");
        }

        // Validate Receiver
        if (shipment.getReceiver() == null) {
            throw new IllegalArgumentException("Receiver information is required.");
        }
        if (isBlank(shipment.getReceiver().getName())) {
            throw new IllegalArgumentException("Receiver name cannot be empty.");
        }
        if (isBlank(shipment.getReceiver().getPhone())) {
            throw new IllegalArgumentException("Receiver phone number is required.");
        }
        if (isBlank(shipment.getReceiver().getAddress())) {
            throw new IllegalArgumentException("Receiver address is required.");
        }

        // Validate Parcel
        if (isBlank(shipment.getParcelDescription())) {
            throw new IllegalArgumentException("Parcel description is required.");
        }
        if (shipment.getWeightKg() <= 0) {
            throw new IllegalArgumentException("Parcel weight must be greater than 0 kg.");
        }
        if (shipment.getDeclaredValue() < 0) {
            throw new IllegalArgumentException("Declared value cannot be negative.");
        }
    }

    // ── Lookup & Search ─────────────────────────────────────────────────────────

    public Shipment trackShipment(String tn) throws SQLException {
        if (isBlank(tn)) {
            throw new IllegalArgumentException("Please enter a tracking number.");
        }
        return shipmentDAO.findByTrackingNumber(tn.trim().toUpperCase());
    }

    public List<StatusLog> getShipmentHistory(String tn) throws SQLException {
        if (isBlank(tn)) {
            throw new IllegalArgumentException("Please enter a tracking number.");
        }
        return statusLogDAO.findByTrackingNumber(tn.trim().toUpperCase());
    }

    public List<Shipment> getAllShipments() throws SQLException {
        return shipmentDAO.findAll();
    }

    public List<Shipment> getRecentShipments(int limit) throws SQLException {
        return shipmentDAO.findRecentShipments(limit);
    }

    public List<Shipment> getShipmentsByStatus(ShipmentStatus s) throws SQLException {
        return shipmentDAO.findByStatus(s);
    }

    public List<Shipment> searchShipments(String query) throws SQLException {
        if (isBlank(query)) {
            return shipmentDAO.findAll();
        }
        return shipmentDAO.search(query.trim());
    }

    public List<Shipment> searchBySender(String name) throws SQLException {
        if (isBlank(name)) throw new IllegalArgumentException("Search query cannot be empty.");
        return shipmentDAO.findBySenderName(name.trim());
    }

    public List<Shipment> searchByReceiver(String name) throws SQLException {
        if (isBlank(name)) throw new IllegalArgumentException("Search query cannot be empty.");
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

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
