package com.courier;

import com.courier.database.DatabaseManager;
import com.courier.model.Person;
import com.courier.model.Shipment;
import com.courier.model.ShipmentStatus;
import com.courier.model.StatusLog;
import com.courier.service.ShipmentService;
import com.courier.util.TrackingNumberGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ShipmentService and TrackingNumberGenerator.
 */
public class ShipmentServiceTest {

    private static ShipmentService service;

    @BeforeAll
    public static void setUp() {
        // Initialize DB
        DatabaseManager.getInstance();
        service = new ShipmentService();
    }

    private Shipment createValidShipment() {
        Person sender = new Person("Alice Johnson", "12 Maple Street", "Boston", "MA", "02108", "+1 617-555-0199", "alice@test.com");
        Person receiver = new Person("Bob Miller", "456 Oak Avenue", "Chicago", "IL", "60601", "+1 312-555-0144", "bob@test.com");

        Shipment shipment = new Shipment();
        shipment.setSender(sender);
        shipment.setReceiver(receiver);
        shipment.setParcelDescription("Software Engineering Textbooks");
        shipment.setParcelCategory("Documents");
        shipment.setWeightKg(3.2);
        shipment.setDeclaredValue(120.0);
        shipment.setServiceType("Express");
        shipment.setCurrentLocation("Boston Sorting Center");
        return shipment;
    }

    @Test
    @DisplayName("Should successfully dispatch valid shipment and create tracking history")
    public void testDispatchShipmentSuccess() throws SQLException {
        Shipment shipment = createValidShipment();
        Shipment dispatched = service.dispatchShipment(shipment);

        assertNotNull(dispatched.getTrackingNumber(), "Tracking number should be generated");
        assertTrue(dispatched.getTrackingNumber().startsWith("CTS-"), "Tracking number should have prefix CTS-");
        assertEquals(ShipmentStatus.DISPATCHED, dispatched.getStatus());
        assertNotNull(dispatched.getEstimatedDelivery(), "ETA must be calculated");

        // Verify lookup
        Shipment tracked = service.trackShipment(dispatched.getTrackingNumber());
        assertNotNull(tracked, "Shipment should be retrievable from database");
        assertEquals("Alice Johnson", tracked.getSender().getName());
        assertEquals("Bob Miller", tracked.getReceiver().getName());

        // Verify initial audit log entry
        List<StatusLog> logs = service.getShipmentHistory(dispatched.getTrackingNumber());
        assertFalse(logs.isEmpty(), "Initial status log must exist");
        assertEquals(ShipmentStatus.DISPATCHED, logs.get(0).getStatus());
    }

    @Test
    @DisplayName("Should reject shipment with missing sender name")
    public void testValidationMissingSenderName() {
        Shipment shipment = createValidShipment();
        shipment.getSender().setName("");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.dispatchShipment(shipment);
        });
        assertTrue(ex.getMessage().contains("Sender name"));
    }

    @Test
    @DisplayName("Should reject shipment with non-positive weight")
    public void testValidationInvalidWeight() {
        Shipment shipment = createValidShipment();
        shipment.setWeightKg(0.0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.dispatchShipment(shipment);
        });
        assertTrue(ex.getMessage().contains("weight"));
    }

    @Test
    @DisplayName("Should successfully update shipment status and record audit log")
    public void testUpdateStatusWorkflow() throws SQLException {
        Shipment shipment = createValidShipment();
        Shipment dispatched = service.dispatchShipment(shipment);
        String tn = dispatched.getTrackingNumber();

        // 1. Move to In Transit
        boolean ok1 = service.updateShipmentStatus(tn, ShipmentStatus.IN_TRANSIT, "Hartford Transit Hub", "Package scanned at waypoint", "Agent Dave");
        assertTrue(ok1);

        Shipment updated1 = service.trackShipment(tn);
        assertEquals(ShipmentStatus.IN_TRANSIT, updated1.getStatus());
        assertEquals("Hartford Transit Hub", updated1.getCurrentLocation());

        // 2. Move to Out For Delivery
        boolean ok2 = service.updateShipmentStatus(tn, ShipmentStatus.OUT_FOR_DELIVERY, "Chicago Hub", "Loaded on van", "Driver Mike");
        assertTrue(ok2);

        // 3. Move to Delivered
        boolean ok3 = service.updateShipmentStatus(tn, ShipmentStatus.DELIVERED, "Delivered to Door", "Signed by Bob", "Driver Mike");
        assertTrue(ok3);

        Shipment finalShipment = service.trackShipment(tn);
        assertEquals(ShipmentStatus.DELIVERED, finalShipment.getStatus());

        // Verify status history count
        List<StatusLog> logs = service.getShipmentHistory(tn);
        assertEquals(4, logs.size(), "Should have 1 dispatch log + 3 update logs");
    }

    @Test
    @DisplayName("Should disallow invalid state transition from DELIVERED to IN_TRANSIT")
    public void testInvalidTransitionFromDelivered() throws SQLException {
        Shipment shipment = createValidShipment();
        Shipment dispatched = service.dispatchShipment(shipment);
        String tn = dispatched.getTrackingNumber();

        // Mark as Delivered
        service.updateShipmentStatus(tn, ShipmentStatus.DELIVERED, "Customer Front Porch", "Delivered", "Courier");

        // Attempt invalid transition back to IN_TRANSIT
        assertThrows(IllegalStateException.class, () -> {
            service.updateShipmentStatus(tn, ShipmentStatus.IN_TRANSIT, "Hub", "Mistaken update", "Staff");
        });
    }

    @Test
    @DisplayName("TrackingNumberGenerator should produce unique formatted IDs")
    public void testTrackingNumberGeneratorUniqueness() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String id = TrackingNumberGenerator.generate();
            assertTrue(id.startsWith("CTS-"), "ID must start with CTS-");
            assertEquals(19, id.length(), "Expected format CTS-YYYYMMDD-XXXXXX (19 characters)");
            assertFalse(generated.contains(id), "Generated tracking ID must be unique");
            generated.add(id);
        }
    }
}
