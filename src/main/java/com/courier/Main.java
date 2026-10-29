package com.courier;

import com.courier.controller.ShipmentController;
import com.courier.dao.ShipmentDAO;
import com.courier.dao.StatusLogDAO;
import com.courier.database.DatabaseManager;
import com.courier.view.DashboardFrame;

import javax.swing.*;

/**
 * Entry point for the Courier and Parcel Tracking System.
 *
 * Implements the MVC (Model-View-Controller) architecture:
 * - Initializes SQLite database connection (DatabaseManager)
 * - Instantiates Data Access Objects (ShipmentDAO, StatusLogDAO)
 * - Instantiates Controller (ShipmentController)
 * - Instantiates View (DashboardFrame)
 * - Wires Controller and View, then displays the GUI on the Event Dispatch Thread (EDT)
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  Courier and Parcel Tracking System");
        System.out.println("  Architecture: Model-View-Controller (MVC)");
        System.out.println("  GUI: Java Swing  |  Database: SQLite (JDBC)");
        System.out.println("=================================================");

        // 1. Initialize SQLite Database & Local Data Directory (courier.db)
        DatabaseManager db = DatabaseManager.getInstance();

        // 2. Register application shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            db.closeConnection();
            System.out.println("[App] SQLite connection closed. Application shutdown complete.");
        }));

        // 3. Apply modern Look & Feel
        setupLookAndFeel();

        // 4. Instantiate and wire MVC components on Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                // DAO Layer
                ShipmentDAO shipmentDAO = new ShipmentDAO();
                StatusLogDAO statusLogDAO = new StatusLogDAO();

                // Controller Layer
                ShipmentController controller = new ShipmentController(shipmentDAO, statusLogDAO);

                // View Layer
                DashboardFrame view = new DashboardFrame(controller);

                // Wire View to Controller
                controller.setView(view);

                // Initial data load and display
                view.refreshAllData();
                view.setVisible(true);

                System.out.println("[App] MVC Desktop GUI initialized and running.");
            } catch (Exception e) {
                System.err.println("[App] Failed to start application: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Applies FlatLaf modern Light theme with seamless fallback to native OS Look and Feel.
     */
    private static void setupLookAndFeel() {
        try {
            Class<?> flatLafClass = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            LookAndFeel flatLaf = (LookAndFeel) flatLafClass.getDeclaredConstructor().newInstance();
            UIManager.setLookAndFeel(flatLaf);
            System.out.println("[UI] FlatLaf modern theme applied.");
        } catch (Throwable t) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                System.out.println("[UI] Native OS theme applied.");
            } catch (Exception ignored) {
                // Default Java Swing theme remains active
            }
        }
    }
}
