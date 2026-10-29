package com.courier;

import com.courier.controller.ShipmentController;
import com.courier.dao.ShipmentDAO;
import com.courier.dao.StatusLogDAO;
import com.courier.database.DatabaseManager;
import com.courier.model.Person;
import com.courier.model.Shipment;
import com.courier.model.ShipmentStatus;
import com.courier.model.StatusLog;
import com.courier.util.TrackingNumberGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying ShipmentController operations in the MVC pattern.
 */
public class ShipmentControllerTest {

    private static ShipmentController controller;

    @BeforeAll
    public static void setup() {
        DatabaseManager.getInstance();
        ShipmentDAO shipmentDAO = new ShipmentDAO();
        StatusLogDAO statusLogDAO = new StatusLogDAO();
        controller = new ShipmentController(shipmentDAO, statusLogDAO);
    }

    private Shipment createTestShipment() {
        Person sender = new Person("Neha Sharma", "101 MG Road", "Bengaluru", "Karnataka", "560001", "+91 98450 11223", "neha@test.com");
        Person receiver = new Person("Rohan Verma", "502 Park View", "Pune", "Maharashtra", "411001", "+91 98230 44556", "rohan@test.com");

        Shipment s = new Shipment();
        s.setSender(sender);
        s.setReceiver(receiver);
        s.setParcelDescription("Academic Textbooks & Notes");
        s.setParcelCategory("Documents");
        s.setWeightKg(1.8);
        s.setDeclaredValue(1500.0);
        s.setServiceType("Express");
        s.setCurrentLocation("Bengaluru Central Hub");
        return s;
    }

    @Test
    @DisplayName("Controller should successfully dispatch shipment and calculate ETA")
    public void testDispatchShipment() throws SQLException {
        Shipment s = createTestShipment();
        Shipment dispatched = controller.dispatchShipment(s);

        assertNotNull(dispatched.getTrackingNumber());
        assertTrue(dispatched.getTrackingNumber().startsWith("CTS-"));
        assertEquals(ShipmentStatus.DISPATCHED, dispatched.getStatus());
        assertNotNull(dispatched.getEstimatedDelivery());

        // Verify tracking via controller
        Shipment tracked = controller.trackShipment(dispatched.getTrackingNumber());
        assertNotNull(tracked);
        assertEquals("Neha Sharma", tracked.getSender().getName());
        assertEquals("Rohan Verma", tracked.getReceiver().getName());

        // Verify history log via controller
        List<StatusLog> logs = controller.getShipmentHistory(dispatched.getTrackingNumber());
        assertFalse(logs.isEmpty());
        assertEquals(ShipmentStatus.DISPATCHED, logs.get(0).getStatus());
    }

    @Test
    @DisplayName("Controller should enforce validation on missing sender phone")
    public void testValidationMissingPhone() {
        Shipment s = createTestShipment();
        s.getSender().setPhone("");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            controller.dispatchShipment(s);
        });
        assertTrue(ex.getMessage().contains("phone"));
    }

    @Test
    @DisplayName("Controller should update status and record chronological audit history")
    public void testStatusUpdateAndAudit() throws SQLException {
        Shipment s = createTestShipment();
        Shipment dispatched = controller.dispatchShipment(s);
        String tn = dispatched.getTrackingNumber();

        // 1. Update to IN_TRANSIT
        boolean ok1 = controller.updateShipmentStatus(tn, ShipmentStatus.IN_TRANSIT, "Belagavi Transit Hub", "Scanned at transit stop", "Staff A");
        assertTrue(ok1);

        // 2. Update to OUT_FOR_DELIVERY
        boolean ok2 = controller.updateShipmentStatus(tn, ShipmentStatus.OUT_FOR_DELIVERY, "Pune West Delivery Center", "Loaded on van", "Driver D");
        assertTrue(ok2);

        // 3. Update to DELIVERED
        boolean ok3 = controller.updateShipmentStatus(tn, ShipmentStatus.DELIVERED, "Customer Front Desk", "Delivered to Rohan Verma", "Driver D");
        assertTrue(ok3);

        Shipment finalShipment = controller.trackShipment(tn);
        assertEquals(ShipmentStatus.DELIVERED, finalShipment.getStatus());

        List<StatusLog> history = controller.getShipmentHistory(tn);
        assertEquals(4, history.size()); // 1 initial + 3 updates
    }

    @Test
    @DisplayName("Controller should calculate accurate ETAs for service types")
    public void testCalculateEstimatedDelivery() {
        assertNotNull(controller.calculateEstimatedDelivery("Same-Day"));
        assertNotNull(controller.calculateEstimatedDelivery("Express"));
        assertNotNull(controller.calculateEstimatedDelivery("Standard"));
        assertNotNull(controller.calculateEstimatedDelivery("Overnight"));
        assertNotNull(controller.calculateEstimatedDelivery("International"));
    }

    @Test
    @DisplayName("Controller should return aggregated dashboard statistics")
    public void testDashboardStatistics() throws SQLException {
        ShipmentController.DashboardStats stats = controller.getDashboardStatistics();
        assertNotNull(stats);
        assertTrue(stats.total >= 0);
        assertTrue(stats.successRate >= 0.0 && stats.successRate <= 100.0);
    }
}
