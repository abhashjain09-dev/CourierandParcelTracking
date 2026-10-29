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
     * Registers a new shipment dispatch, generates a tracking number,
     * and logs the initial DISPATCHED event.
     */
    public Shipment dispatchShipment(Shipment shipment) throws SQLException {
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
        shipment.setId(id);

        statusLogDAO.insertLog(new StatusLog(
            shipment.getTrackingNumber(),
            ShipmentStatus.DISPATCHED,
            shipment.getCurrentLocation() != null ? shipment.getCurrentLocation() : "Origin Hub",
            "Shipment dispatched successfully.",
            "System"
        ));
        return shipment;
    }

    /**
     * Updates the status of an existing shipment and adds an audit log entry.
     */
    public boolean updateShipmentStatus(String trackingNumber, ShipmentStatus newStatus,
                                        String location, String remarks, String updatedBy)
            throws SQLException {
        Shipment existing = shipmentDAO.findByTrackingNumber(trackingNumber);
        if (existing == null) return false;

        if (existing.getStatus() == ShipmentStatus.DELIVERED
                && newStatus != ShipmentStatus.RETURNED) {
            throw new IllegalStateException("Shipment already DELIVERED. Only RETURNED is allowed next.");
        }

        boolean updated = shipmentDAO.updateStatus(trackingNumber, newStatus, location, remarks);
        if (updated) {
            statusLogDAO.insertLog(new StatusLog(trackingNumber, newStatus, location, remarks, updatedBy));
        }
        return updated;
    }

    public Shipment        trackShipment(String tn)            throws SQLException { return shipmentDAO.findByTrackingNumber(tn); }
    public List<StatusLog> getShipmentHistory(String tn)       throws SQLException { return statusLogDAO.findByTrackingNumber(tn); }
    public List<Shipment>  getAllShipments()                    throws SQLException { return shipmentDAO.findAll(); }
    public List<Shipment>  getShipmentsByStatus(ShipmentStatus s) throws SQLException { return shipmentDAO.findByStatus(s); }
    public List<Shipment>  searchBySender(String name)         throws SQLException { return shipmentDAO.findBySenderName(name); }
    public List<Shipment>  searchByReceiver(String name)       throws SQLException { return shipmentDAO.findByReceiverName(name); }
    public List<StatusLog> getRecentActivity(int limit)        throws SQLException { return statusLogDAO.findRecentLogs(limit); }
    public boolean         deleteShipment(String tn)           throws SQLException { return shipmentDAO.deleteShipment(tn); }
    public int             getTotalCount()                      throws SQLException { return shipmentDAO.countAll(); }
    public int             getCountByStatus(ShipmentStatus s)  throws SQLException { return shipmentDAO.countByStatus(s); }
}
