package com.courier.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the SQLite database connection, directory creation,
 * and schema initialization.
 *
 * Stores the database file in a dedicated writable local directory ("./data/").
 */
public class DatabaseManager {

    private static final String DATA_DIRECTORY = "data";
    private static final String DB_FILE_NAME = "courier.db";
    private static DatabaseManager instance;

    private String dbPath;
    private String dbUrl;
    private Connection connection;

    private DatabaseManager() {
        setupDatabasePath();
        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Ensures dedicated writable data folder exists and computes SQLite URL.
     */
    private void setupDatabasePath() {
        File dataDir = new File(DATA_DIRECTORY);
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (created) {
                System.out.println("[DB] Created local application-data directory: " + dataDir.getAbsolutePath());
            }
        }
        File dbFile = new File(dataDir, DB_FILE_NAME);
        this.dbPath = dbFile.getAbsolutePath();
        this.dbUrl = "jdbc:sqlite:" + dbPath;
    }

    public String getDatabasePath() {
        return dbPath;
    }

    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(dbUrl);
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to establish SQLite DB connection: " + e.getMessage(), e);
        }
        return connection;
    }

    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbUrl);
            connection.setAutoCommit(true);
            createTables();
            seedSampleDataIfEmpty();
            System.out.println("[DB] SQLite database active at: " + dbPath);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found on classpath.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Shipments table
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

            // Tracking history audit log table
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

            // Indexes for fast lookups
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tracking  ON shipments(tracking_number)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_status    ON shipments(status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sender    ON shipments(sender_name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_receiver  ON shipments(receiver_name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_log_track ON status_logs(tracking_number)");
        }
    }

    /**
     * Seeds initial realistic sample records on brand-new installs
     * so that the Dashboard and Tracking table immediately showcase the system's capabilities.
     */
    private void seedSampleDataIfEmpty() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM shipments")) {
            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("[DB] Seeding initial demo shipments...");
                insertSeedShipment(
                    stmt,
                    "CTS-20261025-AX8912", "DELIVERED", "Express", "Apple MacBook Air M3 & Magic Mouse", "Electronics",
                    2.1, 115000.0, 1, "Delivered to Residence", "Package received by recipient with signature",
                    "2026-10-25T09:30:00", "2026-10-27T17:00:00", "2026-10-27T14:15:22",
                    "Rahul Sharma", "Flat 402, Green Valley Apartments", "Mumbai", "Maharashtra", "400050", "+91 98201 12345", "rahul.sharma@example.com",
                    "Priya Patel", "B-12, Sunrise Residency, Sector 15", "Bengaluru", "Karnataka", "560001", "+91 98450 67890", "priya.p@example.com"
                );
                insertSeedLog(stmt, "CTS-20261025-AX8912", "DISPATCHED", "Mumbai Central Hub", "Package booked and sorted for dispatch", "Dispatcher A", "2026-10-25T09:30:00");
                insertSeedLog(stmt, "CTS-20261025-AX8912", "IN_TRANSIT", "Pune Transit Sorting Hub", "In transit via Air Freight flight AI-804", "Logistics Agent", "2026-10-25T21:45:00");
                insertSeedLog(stmt, "CTS-20261025-AX8912", "IN_TRANSIT", "Bengaluru Air Cargo Center", "Arrived at destination hub", "Cargo Ops", "2026-10-26T11:20:00");
                insertSeedLog(stmt, "CTS-20261025-AX8912", "OUT_FOR_DELIVERY", "Bengaluru South Delivery Hub", "Assigned to delivery agent Vikram Singh", "Hub Manager", "2026-10-27T08:30:00");
                insertSeedLog(stmt, "CTS-20261025-AX8912", "DELIVERED", "Delivered to Residence", "Package delivered and signed by Priya Patel", "Agent Vikram", "2026-10-27T14:15:22");

                insertSeedShipment(
                    stmt,
                    "CTS-20261028-PL4420", "IN_TRANSIT", "Standard", "Confidential Legal Contracts & Dossier", "Documents",
                    0.4, 2500.0, 0, "Jaipur Interstate Sorting Center", "En route to New Delhi via express ground convoy",
                    "2026-10-28T11:00:00", "2026-11-02T18:00:00", "2026-10-29T06:40:00",
                    "Amitabh Roy", "44 Park Street, Legal Chambers", "Kolkata", "West Bengal", "700016", "+91 98310 99887", "a.roy@lawfirm.in",
                    "Vikramaditya Rao", "7 Connaught Place, Inner Circle", "New Delhi", "Delhi", "110001", "+91 98111 23456", "v.rao@consulting.org"
                );
                insertSeedLog(stmt, "CTS-20261028-PL4420", "DISPATCHED", "Kolkata Hub", "Dispatched from Kolkata sorting facility", "Clerk S", "2026-10-28T11:00:00");
                insertSeedLog(stmt, "CTS-20261028-PL4420", "IN_TRANSIT", "Jaipur Interstate Sorting Center", "Processed through Jaipur automated hub", "Sorter J", "2026-10-29T06:40:00");

                insertSeedShipment(
                    stmt,
                    "CTS-20261029-TR9011", "OUT_FOR_DELIVERY", "Same-Day", "Critical Cardiac Medicine & Ice Pack", "Medicine",
                    1.2, 14200.0, 1, "Hyderabad North Hub", "With Courier Delivery Agent (Van #8)",
                    "2026-10-29T08:00:00", "2026-10-29T16:00:00", "2026-10-29T12:30:00",
                    "Apollo MedExpress", "Plot 10, HITEC City", "Hyderabad", "Telangana", "500081", "+91 94400 55667", "express@apollomed.com",
                    "Dr. K. Srinivas", "Room 304, Care Hospital", "Secunderabad", "Telangana", "500003", "+91 98490 11223", "ksrinivas@carehospital.org"
                );
                insertSeedLog(stmt, "CTS-20261029-TR9011", "DISPATCHED", "HITEC City Medical Dispatch Hub", "Cold-chain shipment logged", "Pharma Team", "2026-10-29T08:00:00");
                insertSeedLog(stmt, "CTS-20261029-TR9011", "OUT_FOR_DELIVERY", "Hyderabad North Hub", "Loaded on climate-controlled delivery van", "Driver Suresh", "2026-10-29T12:30:00");

                insertSeedShipment(
                    stmt,
                    "CTS-20261029-DS1195", "DISPATCHED", "Express", "Designer Silk Sarees & Ethnic Wear", "Clothing",
                    3.5, 38000.0, 0, "Surat Textile Hub", "Manifest generated, awaiting flight departure",
                    "2026-10-29T13:10:00", "2026-10-31T15:00:00", "2026-10-29T13:10:00",
                    "Meena Textiles", "Shop 104, Ring Road Market", "Surat", "Gujarat", "395002", "+91 98251 77665", "orders@meenasarees.com",
                    "Ananya Sen", "12-B Lake Gardens", "Kolkata", "West Bengal", "700045", "+91 98300 33445", "ananya.sen@gmail.com"
                );
                insertSeedLog(stmt, "CTS-20261029-DS1195", "DISPATCHED", "Surat Textile Hub", "Package booked and barcode assigned", "Surat Dispatcher", "2026-10-29T13:10:00");

                System.out.println("[DB] Seed sample data successfully populated.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Seed data error (non-fatal): " + e.getMessage());
        }
    }

    private void insertSeedShipment(Statement st, String tn, String stt, String srv, String desc, String cat,
                                    double wt, double val, int fragile, String loc, String rem,
                                    String disp, String est, String upd,
                                    String sName, String sAddr, String sCity, String sState, String sPin, String sPhone, String sEmail,
                                    String rName, String rAddr, String rCity, String rState, String rPin, String rPhone, String rEmail) throws SQLException {
        String sql = String.format(
            "INSERT INTO shipments (tracking_number, status, service_type, parcel_description, parcel_category, weight_kg, declared_value, is_fragile, current_location, remarks, dispatch_date, estimated_delivery, last_updated, sender_name, sender_address, sender_city, sender_state, sender_pincode, sender_phone, sender_email, receiver_name, receiver_address, receiver_city, receiver_state, receiver_pincode, receiver_phone, receiver_email) " +
            "VALUES ('%s','%s','%s','%s','%s',%f,%f,%d,'%s','%s','%s','%s','%s','%s','%s','%s','%s','%s','%s','%s','%s','%s','%s','%s','%s','%s','%s')",
            tn, stt, srv, desc, cat, wt, val, fragile, loc, rem, disp, est, upd,
            sName, sAddr, sCity, sState, sPin, sPhone, sEmail,
            rName, rAddr, rCity, rState, rPin, rPhone, rEmail
        );
        st.execute(sql);
    }

    private void insertSeedLog(Statement st, String tn, String status, String loc, String rem, String updatedBy, String ts) throws SQLException {
        String sql = String.format(
            "INSERT INTO status_logs (tracking_number, status, location, remarks, updated_by, timestamp) " +
            "VALUES ('%s','%s','%s','%s','%s','%s')",
            tn, status, loc, rem, updatedBy, ts
        );
        st.execute(sql);
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Database connection safely closed.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error closing connection: " + e.getMessage());
        }
    }
}
