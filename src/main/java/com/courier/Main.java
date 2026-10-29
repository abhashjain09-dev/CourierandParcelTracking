package com.courier;

import com.courier.database.DatabaseManager;
import com.courier.service.ShipmentService;
import com.courier.ui.CourierUI;

/**
 * Entry point for the Courier and Parcel Tracking System.
 *
 * Architecture:
 *   model/       - Domain objects (Shipment, Person, StatusLog, ShipmentStatus)
 *   database/    - JDBC DAO layer  (DatabaseManager, ShipmentDAO, StatusLogDAO)
 *   service/     - Business logic  (ShipmentService)
 *   ui/          - Console UI      (CourierUI)
 *   util/        - Helpers         (TrackingNumberGenerator, ConsoleHelper)
 *
 * Database: SQLite  (file: courier_tracking.db)
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("Starting Courier and Parcel Tracking System...");

        DatabaseManager db = DatabaseManager.getInstance();
        ShipmentService service = new ShipmentService();
        CourierUI ui = new CourierUI(service);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            db.closeConnection();
            System.out.println("System shutdown complete.");
        }));

        ui.start();
    }
}
