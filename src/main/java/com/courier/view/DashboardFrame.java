package com.courier.view;

import com.courier.controller.ShipmentController;
import com.courier.database.DatabaseManager;
import com.courier.model.Shipment;
import com.courier.model.ShipmentStatus;
import com.courier.model.StatusLog;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Main application window (View in MVC architecture).
 *
 * Displays Dashboard metrics, All Shipments management table,
 * Track Parcel view, and Dispatch form.
 *
 * All actions and business logic are delegated to ShipmentController.
 */
public class DashboardFrame extends JFrame {

    private final ShipmentController controller;

    private JTabbedPane tabbedPane;
    private TrackingPanel trackingPanel;
    private ShipmentForm shipmentForm;

    // Stat labels
    private JLabel lblTotalVal;
    private JLabel lblDispatchedVal;
    private JLabel lblInTransitVal;
    private JLabel lblOutForDelVal;
    private JLabel lblDeliveredVal;
    private JLabel lblSuccessRateVal;

    // Shipments Table
    private JTable shipmentsTable;
    private DefaultTableModel shipmentsTableModel;
    private JTextField txtSearch;
    private JComboBox<String> cmbStatusFilter;

    // Dashboard Recent Tables
    private JTable recentShipmentsTable;
    private DefaultTableModel recentShipmentsModel;
    private JTable recentLogsTable;
    private DefaultTableModel recentLogsModel;

    // Status Bar
    private JLabel lblStatusMessage;

    public DashboardFrame(ShipmentController controller) {
        this.controller = controller;

        setTitle("Courier and Parcel Tracking System (MVC Architecture)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 800);
        setMinimumSize(new Dimension(980, 650));
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.COLOR_BG_APP);

        // Header
        root.add(createHeaderPanel(), BorderLayout.NORTH);

        // Tabs
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BOLD);
        tabbedPane.setBackground(Color.WHITE);

        // Child Views
        trackingPanel = new TrackingPanel(controller, this::refreshAllData);
        shipmentForm  = new ShipmentForm(controller, () -> {
            refreshAllData();
            tabbedPane.setSelectedIndex(1); // Switch to Shipments table
        });

        tabbedPane.addTab("  📊 Dashboard  ", createDashboardTab());
        tabbedPane.addTab("  📦 All Shipments  ", createShipmentsTab());
        tabbedPane.addTab("  🔍 Track Parcel  ", trackingPanel);
        tabbedPane.addTab("  ➕ Dispatch Shipment  ", shipmentForm);

        root.add(tabbedPane, BorderLayout.CENTER);

        // Bottom Status Bar
        root.add(createStatusBar(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ── Header Bar ──────────────────────────────────────────────────────────────

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(UITheme.COLOR_PRIMARY);
        header.setBorder(new EmptyBorder(12, 20, 12, 20));

        JPanel brandPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        brandPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("COURIER & PARCEL TRACKING SYSTEM");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 19));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Model-View-Controller (MVC) • Java Swing & SQLite Desktop Application");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(203, 213, 225));

        brandPanel.add(lblTitle);
        brandPanel.add(lblSub);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 4));
        rightPanel.setOpaque(false);

        JLabel lblLiveClock = new JLabel();
        lblLiveClock.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblLiveClock.setForeground(new Color(224, 242, 254));

        Timer timer = new Timer(1000, e -> {
            lblLiveClock.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss")));
        });
        timer.start();
        lblLiveClock.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss")));

        JLabel lblDbBadge = new JLabel("● SQLite (courier.db)");
        lblDbBadge.setFont(UITheme.FONT_BOLD);
        lblDbBadge.setForeground(new Color(74, 222, 128));

        JButton btnQuickRefresh = new JButton("↻ Refresh");
        btnQuickRefresh.setFont(UITheme.FONT_BOLD);
        btnQuickRefresh.setBackground(new Color(37, 99, 235));
        btnQuickRefresh.setForeground(Color.WHITE);
        btnQuickRefresh.setFocusPainted(false);
        btnQuickRefresh.setBorder(new EmptyBorder(6, 12, 6, 12));
        btnQuickRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnQuickRefresh.addActionListener(e -> refreshAllData());

        rightPanel.add(lblLiveClock);
        rightPanel.add(lblDbBadge);
        rightPanel.add(btnQuickRefresh);

        header.add(brandPanel, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);
        return header;
    }

    // ── Tab 1: Dashboard ────────────────────────────────────────────────────────

    private JPanel createDashboardTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(UITheme.COLOR_BG_APP);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // 1. Metric Cards
        JPanel statsGrid = new JPanel(new GridLayout(1, 6, 12, 0));
        statsGrid.setOpaque(false);

        lblTotalVal       = new JLabel("0");
        lblDispatchedVal  = new JLabel("0");
        lblInTransitVal   = new JLabel("0");
        lblOutForDelVal   = new JLabel("0");
        lblDeliveredVal   = new JLabel("0");
        lblSuccessRateVal = new JLabel("0%");

        statsGrid.add(createStatCard("Total Parcels", lblTotalVal, new Color(37, 99, 235)));
        statsGrid.add(createStatCard("Dispatched", lblDispatchedVal, new Color(147, 51, 234)));
        statsGrid.add(createStatCard("In Transit", lblInTransitVal, new Color(217, 119, 6)));
        statsGrid.add(createStatCard("Out For Delivery", lblOutForDelVal, new Color(2, 132, 199)));
        statsGrid.add(createStatCard("Delivered", lblDeliveredVal, new Color(22, 163, 74)));
        statsGrid.add(createStatCard("Success Rate", lblSuccessRateVal, new Color(13, 148, 136)));

        panel.add(statsGrid, BorderLayout.NORTH);

        // 2. Split Lower Panels
        JPanel splitPanel = new JPanel(new GridLayout(1, 2, 14, 0));
        splitPanel.setOpaque(false);

        // Left: Recent Shipments
        JPanel leftCard = UITheme.createCardPanel();
        leftCard.setLayout(new BorderLayout(0, 8));

        JPanel leftHeader = new JPanel(new BorderLayout());
        leftHeader.setOpaque(false);
        JLabel lblRecentShip = new JLabel("Recent Shipments");
        lblRecentShip.setFont(UITheme.FONT_SUBTITLE);
        lblRecentShip.setForeground(UITheme.COLOR_TEXT_MAIN);

        JButton btnViewAll = UITheme.createSecondaryButton("View All Shipments →");
        btnViewAll.addActionListener(e -> tabbedPane.setSelectedIndex(1));

        leftHeader.add(lblRecentShip, BorderLayout.WEST);
        leftHeader.add(btnViewAll, BorderLayout.EAST);

        String[] recShipCols = {"Tracking #", "Sender", "Receiver", "Status"};
        recentShipmentsModel = new DefaultTableModel(recShipCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        recentShipmentsTable = new JTable(recentShipmentsModel);
        UITheme.styleTable(recentShipmentsTable);
        recentShipmentsTable.getColumnModel().getColumn(3).setCellRenderer(new UITheme.StatusBadgeRenderer());
        recentShipmentsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && recentShipmentsTable.getSelectedRow() != -1) {
                    int row = recentShipmentsTable.getSelectedRow();
                    String tn = (String) recentShipmentsTable.getValueAt(row, 0);
                    jumpToTracking(tn);
                }
            }
        });

        leftCard.add(leftHeader, BorderLayout.NORTH);
        leftCard.add(new JScrollPane(recentShipmentsTable), BorderLayout.CENTER);

        // Right: Recent Activity Log
        JPanel rightCard = UITheme.createCardPanel();
        rightCard.setLayout(new BorderLayout(0, 8));

        JLabel lblRecentLogs = new JLabel("Recent System Audit Activity");
        lblRecentLogs.setFont(UITheme.FONT_SUBTITLE);
        lblRecentLogs.setForeground(UITheme.COLOR_TEXT_MAIN);

        String[] logCols = {"Timestamp", "Tracking #", "Status", "Location"};
        recentLogsModel = new DefaultTableModel(logCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        recentLogsTable = new JTable(recentLogsModel);
        UITheme.styleTable(recentLogsTable);
        recentLogsTable.getColumnModel().getColumn(2).setCellRenderer(new UITheme.StatusBadgeRenderer());

        rightCard.add(lblRecentLogs, BorderLayout.NORTH);
        rightCard.add(new JScrollPane(recentLogsTable), BorderLayout.CENTER);

        splitPanel.add(leftCard);
        splitPanel.add(rightCard);

        panel.add(splitPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(8, 2));

        JPanel ind = new JPanel();
        ind.setPreferredSize(new Dimension(4, 38));
        ind.setBackground(accent);
        card.add(ind, BorderLayout.WEST);

        JPanel content = new JPanel(new GridLayout(2, 1, 0, 0));
        content.setOpaque(false);

        JLabel lbl = new JLabel(title.toUpperCase());
        lbl.setFont(UITheme.FONT_STAT_LABEL);
        lbl.setForeground(UITheme.COLOR_TEXT_MUTED);

        valueLabel.setFont(UITheme.FONT_STAT_NUM);
        valueLabel.setForeground(accent);

        content.add(lbl);
        content.add(valueLabel);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ── Tab 2: All Shipments Management ─────────────────────────────────────────

    private JPanel createShipmentsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UITheme.COLOR_BG_APP);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Toolbar
        JPanel toolbarCard = UITheme.createCardPanel();
        toolbarCard.setLayout(new BorderLayout(12, 0));

        // Filters
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterPanel.setOpaque(false);

        JLabel lblSearch = new JLabel("Search:");
        lblSearch.setFont(UITheme.FONT_BOLD);
        txtSearch = new JTextField(18);
        txtSearch.setFont(UITheme.FONT_BODY);
        txtSearch.setBorder(new CompoundBorder(
            new LineBorder(UITheme.COLOR_BORDER, 1, true),
            new EmptyBorder(6, 8, 6, 8)
        ));
        txtSearch.setToolTipText("Search by tracking #, sender, receiver, or city...");
        txtSearch.addActionListener(e -> filterShipmentsTable());

        JButton btnSearch = UITheme.createPrimaryButton("Search");
        btnSearch.addActionListener(e -> filterShipmentsTable());

        JLabel lblFilter = new JLabel("Status:");
        lblFilter.setFont(UITheme.FONT_BOLD);
        cmbStatusFilter = new JComboBox<>(new String[]{
            "All Statuses", "DISPATCHED", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED", "FAILED_DELIVERY", "RETURNED", "ON_HOLD"
        });
        cmbStatusFilter.addActionListener(e -> filterShipmentsTable());

        filterPanel.add(lblSearch);
        filterPanel.add(txtSearch);
        filterPanel.add(btnSearch);
        filterPanel.add(Box.createHorizontalStrut(10));
        filterPanel.add(lblFilter);
        filterPanel.add(cmbStatusFilter);

        // Actions
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setOpaque(false);

        JButton btnNewShipment = UITheme.createPrimaryButton("➕ New Shipment");
        JButton btnTrackSelect = UITheme.createSecondaryButton("🔍 Track / View Details");
        JButton btnUpdateStatus = UITheme.createSecondaryButton("✏️ Update Status");
        JButton btnDelete = UITheme.createDangerButton("🗑️ Delete");

        btnNewShipment.addActionListener(e -> tabbedPane.setSelectedIndex(3));
        btnTrackSelect.addActionListener(e -> {
            String tn = getSelectedTrackingNumber();
            if (tn != null) jumpToTracking(tn);
        });
        btnUpdateStatus.addActionListener(e -> openStatusUpdateForSelected());
        btnDelete.addActionListener(e -> deleteSelectedShipment());

        actionPanel.add(btnNewShipment);
        actionPanel.add(btnTrackSelect);
        actionPanel.add(btnUpdateStatus);
        actionPanel.add(btnDelete);

        toolbarCard.add(filterPanel, BorderLayout.WEST);
        toolbarCard.add(actionPanel, BorderLayout.EAST);
        panel.add(toolbarCard, BorderLayout.NORTH);

        // Table
        String[] cols = {
            "Tracking #", "Sender Name", "Receiver Name", "Destination City",
            "Status", "Service", "Weight (kg)", "Dispatched", "Estimated Delivery"
        };
        shipmentsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        shipmentsTable = new JTable(shipmentsTableModel);
        UITheme.styleTable(shipmentsTable);
        shipmentsTable.getColumnModel().getColumn(4).setCellRenderer(new UITheme.StatusBadgeRenderer());
        shipmentsTable.getColumnModel().getColumn(0).setPreferredWidth(160);
        shipmentsTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        shipmentsTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        shipmentsTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        shipmentsTable.getColumnModel().getColumn(4).setPreferredWidth(130);
        shipmentsTable.getColumnModel().getColumn(5).setPreferredWidth(90);
        shipmentsTable.getColumnModel().getColumn(6).setPreferredWidth(80);
        shipmentsTable.getColumnModel().getColumn(7).setPreferredWidth(130);
        shipmentsTable.getColumnModel().getColumn(8).setPreferredWidth(130);

        shipmentsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && shipmentsTable.getSelectedRow() != -1) {
                    String tn = getSelectedTrackingNumber();
                    if (tn != null) jumpToTracking(tn);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(shipmentsTable);
        scrollPane.setBorder(new LineBorder(UITheme.COLOR_BORDER, 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // ── Bottom Status Bar ───────────────────────────────────────────────────────

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(new CompoundBorder(
            new LineBorder(UITheme.COLOR_BORDER, 1),
            new EmptyBorder(6, 16, 6, 16)
        ));

        lblStatusMessage = new JLabel("System Ready.");
        lblStatusMessage.setFont(UITheme.FONT_BODY);
        lblStatusMessage.setForeground(UITheme.COLOR_TEXT_MAIN);

        JLabel lblDbInfo = new JLabel("Database: " + DatabaseManager.getInstance().getDatabasePath());
        lblDbInfo.setFont(UITheme.FONT_BODY);
        lblDbInfo.setForeground(UITheme.COLOR_TEXT_MUTED);

        JLabel lblVer = new JLabel("MVC Desktop • v1.0.0");
        lblVer.setFont(UITheme.FONT_BODY);
        lblVer.setForeground(UITheme.COLOR_TEXT_MUTED);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        right.setOpaque(false);
        right.add(lblDbInfo);
        right.add(lblVer);

        bar.add(lblStatusMessage, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ── Data Synchronization via Controller ─────────────────────────────────────

    public void refreshAllData() {
        try {
            // 1. Metrics from Controller
            ShipmentController.DashboardStats stats = controller.getDashboardStatistics();
            lblTotalVal.setText(String.valueOf(stats.total));
            lblDispatchedVal.setText(String.valueOf(stats.dispatched));
            lblInTransitVal.setText(String.valueOf(stats.inTransit));
            lblOutForDelVal.setText(String.valueOf(stats.outForDelivery));
            lblDeliveredVal.setText(String.valueOf(stats.delivered));
            lblSuccessRateVal.setText(String.format("%.1f%%", stats.successRate));

            // 2. Recent Shipments via Controller
            recentShipmentsModel.setRowCount(0);
            List<Shipment> recentList = controller.getRecentShipments(8);
            for (Shipment s : recentList) {
                recentShipmentsModel.addRow(new Object[]{
                    s.getTrackingNumber(),
                    s.getSender().getName(),
                    s.getReceiver().getName(),
                    s.getStatus().name()
                });
            }

            // 3. Recent Activity Logs via Controller
            recentLogsModel.setRowCount(0);
            List<StatusLog> logList = controller.getRecentLogs(8);
            for (StatusLog l : logList) {
                recentLogsModel.addRow(new Object[]{
                    l.getFormattedTimestamp(),
                    l.getTrackingNumber(),
                    l.getStatus().name(),
                    l.getLocation() != null ? l.getLocation() : "Facility"
                });
            }

            // 4. Shipments Table
            filterShipmentsTable();

            lblStatusMessage.setText("Ready. Database synchronized: " + stats.total + " shipments stored.");
        } catch (SQLException e) {
            lblStatusMessage.setText("Database sync error: " + e.getMessage());
            System.err.println("Database sync error: " + e.getMessage());
        }
    }

    private void filterShipmentsTable() {
        String query = txtSearch != null ? txtSearch.getText().trim() : "";
        String filter = cmbStatusFilter != null ? (String) cmbStatusFilter.getSelectedItem() : "All Statuses";

        try {
            List<Shipment> list = controller.searchShipments(query, filter);
            shipmentsTableModel.setRowCount(0);
            for (Shipment s : list) {
                shipmentsTableModel.addRow(new Object[]{
                    s.getTrackingNumber(),
                    s.getSender().getName(),
                    s.getReceiver().getName(),
                    s.getReceiver().getCity() != null ? s.getReceiver().getCity() : "-",
                    s.getStatus().name(),
                    s.getServiceType(),
                    s.getWeightKg(),
                    s.getFormattedDispatchDate(),
                    s.getFormattedEstimatedDelivery()
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error loading shipments:\n" + e.getMessage(),
                "Query Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getSelectedTrackingNumber() {
        int row = shipmentsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a shipment from the table.",
                "No Shipment Selected",
                JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return (String) shipmentsTable.getValueAt(row, 0);
    }

    private void jumpToTracking(String trackingNumber) {
        tabbedPane.setSelectedIndex(2);
        trackingPanel.loadTracking(trackingNumber);
    }

    private void openStatusUpdateForSelected() {
        String tn = getSelectedTrackingNumber();
        if (tn == null) return;

        try {
            Shipment s = controller.trackShipment(tn);
            if (s == null) return;

            JDialog dialog = new JDialog(this, "Update Status - " + tn, true);
            dialog.setLayout(new BorderLayout(12, 16));
            dialog.getContentPane().setBackground(UITheme.COLOR_BG_APP);

            JPanel form = new JPanel(new GridLayout(4, 2, 10, 12));
            form.setBorder(new EmptyBorder(16, 20, 16, 20));
            form.setOpaque(false);

            JComboBox<ShipmentStatus> cmbStatus = new JComboBox<>(ShipmentStatus.values());
            cmbStatus.setSelectedItem(s.getStatus());

            JTextField txtLoc = new JTextField(s.getCurrentLocation() != null ? s.getCurrentLocation() : "");
            JTextField txtRemarks = new JTextField();
            JTextField txtBy = new JTextField("Admin");

            form.add(new JLabel("New Status:"));
            form.add(cmbStatus);
            form.add(new JLabel("Current Location:"));
            form.add(txtLoc);
            form.add(new JLabel("Remarks / Note:"));
            form.add(txtRemarks);
            form.add(new JLabel("Updated By:"));
            form.add(txtBy);

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
            btnPanel.setOpaque(false);
            JButton btnSave = UITheme.createPrimaryButton("Save Status");
            JButton btnCancel = UITheme.createSecondaryButton("Cancel");

            btnCancel.addActionListener(e -> dialog.dispose());
            btnSave.addActionListener(e -> {
                ShipmentStatus newSt = (ShipmentStatus) cmbStatus.getSelectedItem();
                try {
                    boolean ok = controller.updateShipmentStatus(tn, newSt, txtLoc.getText(), txtRemarks.getText(), txtBy.getText());
                    if (ok) {
                        JOptionPane.showMessageDialog(dialog, "Status updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        refreshAllData();
                    }
                } catch (IllegalStateException | IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Validation Warning", JOptionPane.WARNING_MESSAGE);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            btnPanel.add(btnCancel);
            btnPanel.add(btnSave);

            dialog.add(form, BorderLayout.CENTER);
            dialog.add(btnPanel, BorderLayout.SOUTH);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedShipment() {
        String tn = getSelectedTrackingNumber();
        if (tn == null) return;

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to permanently delete shipment:\n" + tn +
            "\n\nThis will remove the package and all its associated tracking history.",
            "Confirm Delete Shipment",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean deleted = controller.deleteShipment(tn);
                if (deleted) {
                    JOptionPane.showMessageDialog(this, "Shipment deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                    refreshAllData();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete shipment.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
