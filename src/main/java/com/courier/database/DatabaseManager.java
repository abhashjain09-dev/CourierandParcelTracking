package com.courier.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the SQLite database connection and schema initialisation.
 * Uses the Singleton pattern to provide a single shared connection.
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:courier_tracking.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() { initializeDatabase(); }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get DB connection: " + e.getMessage(), e);
        }
        return connection;
    }

    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            connection.setAutoCommit(true);
            createTables();
            System.out.println("[DB] Database initialised: courier_tracking.db");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Database init failed: " + e.getMessage(), e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS shipments (" +
                "  id                  INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  tracking_number     TEXT    NOT NULL UNIQUE," +
                "  status              TEXT    NOT NULL DEFAULT 'DISPATCHED'," +
                "  service_type        TEXT    NOT NULL DEFAULT 'Standard'," +
                "  parcel_description  TEXT," +
                "  parcel_category     TEXT," +
                "  weight_kg           REAL    DEFAULT 0.0," +
                "  declared_value      REAL    DEFAULT 0.0," +
                "  is_fragile          INTEGER DEFAULT 0," +
                "  current_location    TEXT," +
                "  remarks             TEXT," +
                "  dispatch_date       TEXT    NOT NULL," +
                "  estimated_delivery  TEXT," +
                "  last_updated        TEXT    NOT NULL," +
                "  sender_name         TEXT    NOT NULL," +
                "  sender_address      TEXT," +
                "  sender_city         TEXT," +
                "  sender_state        TEXT," +
                "  sender_pincode      TEXT," +
                "  sender_phone        TEXT    NOT NULL," +
                "  sender_email        TEXT," +
                "  receiver_name       TEXT    NOT NULL," +
                "  receiver_address    TEXT," +
                "  receiver_city       TEXT," +
                "  receiver_state      TEXT," +
                "  receiver_pincode    TEXT," +
                "  receiver_phone      TEXT    NOT NULL," +
                "  receiver_email      TEXT" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS status_logs (" +
                "  id              INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  tracking_number TEXT    NOT NULL," +
                "  status          TEXT    NOT NULL," +
                "  location        TEXT," +
                "  remarks         TEXT," +
                "  updated_by      TEXT    DEFAULT 'System'," +
                "  timestamp       TEXT    NOT NULL," +
                "  FOREIGN KEY (tracking_number) REFERENCES shipments(tracking_number)" +
                ")"
            );

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tracking  ON shipments(tracking_number)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_status    ON shipments(status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_log_track ON status_logs(tracking_number)");
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}
