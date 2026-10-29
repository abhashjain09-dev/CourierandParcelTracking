package com.courier.controller;

import com.courier.dao.ShipmentDAO;
import com.courier.dao.StatusLogDAO;
import com.courier.model.Shipment;
import com.courier.model.ShipmentStatus;
import com.courier.model.StatusLog;
import com.courier.util.TrackingNumberGenerator;
import com.courier.view.DashboardFrame;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller in the MVC (Model-View-Controller) architecture.
 *
 * Responsibilities:
 * - Coordinates actions between the Swing Views and DAO/Model persistence layer.
 * - Handles input validation, business logic, state transitions, and error management.
 * - Ensures that View classes do not execute direct SQL queries.
 */
public class ShipmentController {

    private final ShipmentDAO shipmentDAO;
    private final StatusLogDAO statusLogDAO;
    private DashboardFrame view;

    public ShipmentController(ShipmentDAO shipmentDAO, StatusLogDAO statusLogDAO) {
        this.shipmentDAO = shipmentDAO;
        this.statusLogDAO = statusLogDAO;
    }

    public void setView(DashboardFrame view) {
        this.view = view;
    }

    // ── Dispatch (Create) Shipment ──────────────────────────────────────────────

    /**
     * Validates input fields, assigns a unique tracking number, computes the ETA,
     * persists the shipment via JDBC, and logs the initial DISPATCHED event.
     */
    public Shipment dispatchShipment(Shipment shipment) throws IllegalArgumentException, SQLException {
        validateShipment(shipment);

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
            throw new SQLException("Failed to persist shipment into SQLite database.");
        }
        shipment.setId(id);

        String initialLoc = isBlank(shipment.getCurrentLocation()) ? "Origin Hub" : shipment.getCurrentLocation().trim();
        statusLogDAO.insertLog(new StatusLog(
            shipment.getTrackingNumber(),
            ShipmentStatus.DISPATCHED,
            initialLoc,
            "Package registered and dispatched from origin facility.",
            "System Dispatcher"
        ));

        // Refresh UI if view is attached
        if (view != null) {
            view.refreshAllData();
        }

        return shipment;
    }

    // ── Track Shipment ──────────────────────────────────────────────────────────

    public Shipment trackShipment(String trackingNumber) throws IllegalArgumentException, SQLException {
        if (isBlank(trackingNumber)) {
            throw new IllegalArgumentException("Tracking number cannot be empty.");
        }
        return shipmentDAO.findByTrackingNumber(trackingNumber.trim().toUpperCase());
    }

    public List<StatusLog> getShipmentHistory(String trackingNumber) throws IllegalArgumentException, SQLException {
        if (isBlank(trackingNumber)) {
            throw new IllegalArgumentException("Tracking number cannot be empty.");
        }
        return statusLogDAO.findByTrackingNumber(trackingNumber.trim().toUpperCase());
    }

    // ── Update Shipment Status ──────────────────────────────────────────────────

    public boolean updateShipmentStatus(String trackingNumber, ShipmentStatus newStatus,
                                        String location, String remarks, String updatedBy)
            throws IllegalArgumentException, IllegalStateException, SQLException {
        if (isBlank(trackingNumber)) {
            throw new IllegalArgumentException("Tracking number cannot be empty.");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("Please select a new status.");
        }

        String safeLocation  = isBlank(location)  ? "In Transit" : location.trim();
        String safeRemarks   = isBlank(remarks)   ? "Status updated to " + newStatus.getDisplayName() : remarks.trim();
        String safeUpdatedBy = isBlank(updatedBy) ? "Staff" : updatedBy.trim();

        Shipment existing = shipmentDAO.findByTrackingNumber(trackingNumber.trim().toUpperCase());
        if (existing == null) {
            return false;
        }

        // Enforce business state machine
        if (existing.getStatus() == ShipmentStatus.DELIVERED && newStatus != ShipmentStatus.RETURNED) {
            throw new IllegalStateException("Shipment is already DELIVERED. Only 'RETURNED' status is allowed.");
        }
        if (existing.getStatus() == ShipmentStatus.RETURNED) {
            throw new IllegalStateException("Shipment is RETURNED and cannot be modified further.");
        }

        boolean updated = shipmentDAO.updateStatus(trackingNumber.trim().toUpperCase(), newStatus, safeLocation, safeRemarks);
        if (updated) {
            statusLogDAO.insertLog(new StatusLog(
                trackingNumber.trim().toUpperCase(),
                newStatus,
                safeLocation,
                safeRemarks,
                safeUpdatedBy
            ));
            if (view != null) {
                view.refreshAllData();
            }
        }
        return updated;
    }

    // ── Delete Shipment ─────────────────────────────────────────────────────────

    public boolean deleteShipment(String trackingNumber) throws IllegalArgumentException, SQLException {
        if (isBlank(trackingNumber)) {
            throw new IllegalArgumentException("Tracking number cannot be empty.");
        }
        boolean deleted = shipmentDAO.deleteShipment(trackingNumber.trim().toUpperCase());
        if (deleted && view != null) {
            view.refreshAllData();
        }
        return deleted;
    }

    // ── Search & Filter ─────────────────────────────────────────────────────────

    public List<Shipment> searchShipments(String query, String statusFilter) throws SQLException {
        if (!isBlank(query)) {
            return shipmentDAO.search(query.trim());
        }
        if (!isBlank(statusFilter) && !"All Statuses".equalsIgnoreCase(statusFilter)) {
            return shipmentDAO.findByStatus(ShipmentStatus.valueOf(statusFilter));
        }
        return shipmentDAO.findAll();
    }

    public List<Shipment> getRecentShipments(int limit) throws SQLException {
        return shipmentDAO.findRecentShipments(limit);
    }

    public List<StatusLog> getRecentLogs(int limit) throws SQLException {
        return statusLogDAO.findRecentLogs(limit);
    }

    // ── Dashboard Statistics ────────────────────────────────────────────────────

    public DashboardStats getDashboardStatistics() throws SQLException {
        int total      = shipmentDAO.countAll();
        int dispatched = shipmentDAO.countByStatus(ShipmentStatus.DISPATCHED);
        int inTransit  = shipmentDAO.countByStatus(ShipmentStatus.IN_TRANSIT);
        int outForDel  = shipmentDAO.countByStatus(ShipmentStatus.OUT_FOR_DELIVERY);
        int delivered  = shipmentDAO.countByStatus(ShipmentStatus.DELIVERED);
        double rate    = total > 0 ? ((double) delivered / total * 100.0) : 0.0;

        return new DashboardStats(total, dispatched, inTransit, outForDel, delivered, rate);
    }

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

    private void validateShipment(Shipment shipment) {
        if (shipment == null) {
            throw new IllegalArgumentException("Shipment data cannot be empty.");
        }
        if (shipment.getSender() == null || isBlank(shipment.getSender().getName())) {
            throw new IllegalArgumentException("Sender name is required.");
        }
        if (isBlank(shipment.getSender().getPhone())) {
            throw new IllegalArgumentException("Sender phone number is required.");
        }
        if (isBlank(shipment.getSender().getAddress())) {
            throw new IllegalArgumentException("Sender physical address is required.");
        }
        if (shipment.getReceiver() == null || isBlank(shipment.getReceiver().getName())) {
            throw new IllegalArgumentException("Receiver name is required.");
        }
        if (isBlank(shipment.getReceiver().getPhone())) {
            throw new IllegalArgumentException("Receiver phone number is required.");
        }
        if (isBlank(shipment.getReceiver().getAddress())) {
            throw new IllegalArgumentException("Receiver physical address is required.");
        }
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

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ── DTO for Dashboard Statistics ────────────────────────────────────────────

    public static class DashboardStats {
        public final int total;
        public final int dispatched;
        public final int inTransit;
        public final int outForDelivery;
        public final int delivered;
        public final double successRate;

        public DashboardStats(int total, int dispatched, int inTransit,
                              int outForDelivery, int delivered, double successRate) {
            this.total = total;
            this.dispatched = dispatched;
            this.inTransit = inTransit;
            this.outForDelivery = outForDelivery;
            this.delivered = delivered;
            this.successRate = successRate;
        }
    }
}
