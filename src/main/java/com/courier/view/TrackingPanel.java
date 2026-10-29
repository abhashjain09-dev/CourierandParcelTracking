package com.courier.view;

import com.courier.controller.ShipmentController;
import com.courier.model.Person;
import com.courier.model.Shipment;
import com.courier.model.ShipmentStatus;
import com.courier.model.StatusLog;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * View component (Java Swing) for tracking parcels and rendering progress stepper in MVC.
 * Delegates querying, history retrieval, and status changes to ShipmentController.
 */
public class TrackingPanel extends JPanel {

    private final ShipmentController controller;
    private final Runnable onDataChanged;

    private JTextField txtTrackingNumber;
    private JButton btnTrack;
    private JButton btnClear;
    private JButton btnUpdateStatus;

    // Shipment overview components
    private JLabel lblStatusBadge;
    private JLabel lblTrackingId;
    private JLabel lblServiceType;
    private JLabel lblCurrentLocation;
    private JLabel lblEstimatedDelivery;

    private JLabel lblSenderName;
    private JLabel lblSenderPhone;
    private JLabel lblSenderAddress;

    private JLabel lblReceiverName;
    private JLabel lblReceiverPhone;
    private JLabel lblReceiverAddress;

    private JLabel lblParcelDesc;
    private JLabel lblParcelWeight;
    private JLabel lblParcelDeclared;

    private StatusStepperPanel stepperPanel;
    private JTable historyTable;
    private DefaultTableModel historyTableModel;

    private Shipment currentShipment;

    public TrackingPanel(ShipmentController controller, Runnable onDataChanged) {
        this.controller = controller;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.COLOR_BG_APP);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        initComponents();
    }

    private void initComponents() {
        // ── Search Bar ──────────────────────────────────────────────────────────
        JPanel searchCard = UITheme.createCardPanel();
        searchCard.setLayout(new BorderLayout(12, 0));

        JLabel lblSearchPrompt = new JLabel("Tracking Number:");
        lblSearchPrompt.setFont(UITheme.FONT_BOLD);
        lblSearchPrompt.setForeground(UITheme.COLOR_TEXT_MAIN);

        txtTrackingNumber = new JTextField();
        txtTrackingNumber.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtTrackingNumber.setBorder(new CompoundBorder(
            new LineBorder(UITheme.COLOR_BORDER, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        txtTrackingNumber.setToolTipText("Enter tracking number, e.g. CTS-20261025-AX8912");

        btnTrack = UITheme.createPrimaryButton("Track Parcel");
        btnClear = UITheme.createSecondaryButton("Clear");

        btnTrack.addActionListener(e -> performTracking());
        txtTrackingNumber.addActionListener(e -> performTracking());
        btnClear.addActionListener(e -> clearView());

        JPanel searchControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchControls.setOpaque(false);
        searchControls.add(btnTrack);
        searchControls.add(btnClear);

        searchCard.add(lblSearchPrompt, BorderLayout.WEST);
        searchCard.add(txtTrackingNumber, BorderLayout.CENTER);
        searchCard.add(searchControls, BorderLayout.EAST);

        add(searchCard, BorderLayout.NORTH);

        // ── Content (Stepper, Cards, Timeline) ──────────────────────────────────
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);

        // 1. Progress Stepper
        stepperPanel = new StatusStepperPanel();
        mainContent.add(stepperPanel);
        mainContent.add(Box.createVerticalStrut(14));

        // 2. Info Cards Grid (3 Columns)
        JPanel cardsGrid = new JPanel(new GridLayout(1, 3, 14, 0));
        cardsGrid.setOpaque(false);
        cardsGrid.add(createStatusInfoCard());
        cardsGrid.add(createPersonCard("Sender (Origin)", true));
        cardsGrid.add(createPersonCard("Receiver (Destination)", false));

        mainContent.add(cardsGrid);
        mainContent.add(Box.createVerticalStrut(14));

        // 3. Parcel Details & Action Bar
        JPanel parcelCard = UITheme.createCardPanel();
        parcelCard.setLayout(new BorderLayout(10, 6));

        JPanel parcelInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 4));
        parcelInfoPanel.setOpaque(false);

        lblParcelDesc     = new JLabel("Description: -");
        lblParcelWeight   = new JLabel("Weight: -");
        lblParcelDeclared = new JLabel("Declared Value: -");

        lblParcelDesc.setFont(UITheme.FONT_BODY);
        lblParcelWeight.setFont(UITheme.FONT_BODY);
        lblParcelDeclared.setFont(UITheme.FONT_BODY);

        parcelInfoPanel.add(lblParcelDesc);
        parcelInfoPanel.add(new JSeparator(SwingConstants.VERTICAL));
        parcelInfoPanel.add(lblParcelWeight);
        parcelInfoPanel.add(new JSeparator(SwingConstants.VERTICAL));
        parcelInfoPanel.add(lblParcelDeclared);

        btnUpdateStatus = UITheme.createPrimaryButton("Update Shipment Status");
        btnUpdateStatus.setEnabled(false);
        btnUpdateStatus.addActionListener(e -> openUpdateStatusDialog());

        parcelCard.add(parcelInfoPanel, BorderLayout.CENTER);
        parcelCard.add(btnUpdateStatus, BorderLayout.EAST);

        mainContent.add(parcelCard);
        mainContent.add(Box.createVerticalStrut(14));

        // 4. Tracking History Table
        JPanel historyPanel = UITheme.createCardPanel();
        historyPanel.setLayout(new BorderLayout(0, 8));

        JLabel lblHistTitle = new JLabel("Audit & Tracking History Log");
        lblHistTitle.setFont(UITheme.FONT_SUBTITLE);
        lblHistTitle.setForeground(UITheme.COLOR_TEXT_MAIN);

        String[] cols = {"Timestamp", "Status", "Location", "Remarks / Activity", "Updated By"};
        historyTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        historyTable = new JTable(historyTableModel);
        UITheme.styleTable(historyTable);
        historyTable.getColumnModel().getColumn(1).setCellRenderer(new UITheme.StatusBadgeRenderer());
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(160);
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(260);
        historyTable.getColumnModel().getColumn(4).setPreferredWidth(110);

        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.setBorder(new LineBorder(UITheme.COLOR_BORDER, 1));
        scrollPane.setPreferredSize(new Dimension(800, 200));

        historyPanel.add(lblHistTitle, BorderLayout.NORTH);
        historyPanel.add(scrollPane, BorderLayout.CENTER);

        mainContent.add(historyPanel);

        JScrollPane mainScroll = new JScrollPane(mainContent);
        mainScroll.setBorder(null);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(mainScroll, BorderLayout.CENTER);
    }

    private JPanel createStatusInfoCard() {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new GridLayout(6, 1, 0, 4));

        JLabel title = new JLabel("Shipment Status");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.COLOR_PRIMARY);

        lblStatusBadge       = new JLabel("Awaiting Tracking #");
        lblStatusBadge.setFont(UITheme.FONT_BOLD);
        lblTrackingId        = new JLabel("Tracking ID: -");
        lblServiceType       = new JLabel("Service: -");
        lblCurrentLocation   = new JLabel("Location: -");
        lblEstimatedDelivery = new JLabel("Estimated Delivery: -");

        card.add(title);
        card.add(lblStatusBadge);
        card.add(lblTrackingId);
        card.add(lblServiceType);
        card.add(lblCurrentLocation);
        card.add(lblEstimatedDelivery);
        return card;
    }

    private JPanel createPersonCard(String titleText, boolean isSender) {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new GridLayout(4, 1, 0, 4));

        JLabel title = new JLabel(titleText);
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.COLOR_TEXT_MAIN);

        JLabel lblName    = new JLabel("Name: -");
        JLabel lblPhone   = new JLabel("Phone: -");
        JLabel lblAddress = new JLabel("Address: -");

        lblName.setFont(UITheme.FONT_BODY);
        lblPhone.setFont(UITheme.FONT_BODY);
        lblAddress.setFont(UITheme.FONT_BODY);

        if (isSender) {
            lblSenderName    = lblName;
            lblSenderPhone   = lblPhone;
            lblSenderAddress = lblAddress;
        } else {
            lblReceiverName    = lblName;
            lblReceiverPhone   = lblPhone;
            lblReceiverAddress = lblAddress;
        }

        card.add(title);
        card.add(lblName);
        card.add(lblPhone);
        card.add(lblAddress);
        return card;
    }

    public void loadTracking(String trackingNumber) {
        txtTrackingNumber.setText(trackingNumber);
        performTracking();
    }

    /**
     * User action: View delegates parcel lookup to ShipmentController.
     */
    private void performTracking() {
        String tn = txtTrackingNumber.getText().trim();
        if (tn.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter a tracking number.",
                "Tracking Number Required",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Shipment s = controller.trackShipment(tn);
            if (s == null) {
                JOptionPane.showMessageDialog(this,
                    "No shipment found matching tracking number:\n" + tn,
                    "Shipment Not Found",
                    JOptionPane.INFORMATION_MESSAGE);
                clearView();
                return;
            }

            displayShipment(s);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Input Warning", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Database error while querying shipment:\n" + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void displayShipment(Shipment s) {
        this.currentShipment = s;
        btnUpdateStatus.setEnabled(true);

        lblStatusBadge.setText(s.getStatus().getDisplayName());
        lblTrackingId.setText("Tracking ID: " + s.getTrackingNumber());
        lblServiceType.setText("Service: " + s.getServiceType());
        lblCurrentLocation.setText("Location: " + (s.getCurrentLocation() != null ? s.getCurrentLocation() : "Origin"));
        lblEstimatedDelivery.setText("ETA: " + s.getFormattedEstimatedDelivery());

        Person sender = s.getSender();
        lblSenderName.setText("Name: " + sender.getName());
        lblSenderPhone.setText("Phone: " + sender.getPhone());
        lblSenderAddress.setText("Address: " + sender.getFullAddress());

        Person receiver = s.getReceiver();
        lblReceiverName.setText("Name: " + receiver.getName());
        lblReceiverPhone.setText("Phone: " + receiver.getPhone());
        lblReceiverAddress.setText("Address: " + receiver.getFullAddress());

        lblParcelDesc.setText("Description: " + s.getParcelDescription());
        lblParcelWeight.setText("Weight: " + s.getWeightKg() + " kg (" + (s.isFragile() ? "Fragile" : "Standard") + ")");
        lblParcelDeclared.setText("Declared Value: Rs. " + String.format("%.2f", s.getDeclaredValue()));

        stepperPanel.setStatus(s.getStatus());

        // Refresh History Table via Controller
        historyTableModel.setRowCount(0);
        try {
            List<StatusLog> logs = controller.getShipmentHistory(s.getTrackingNumber());
            for (StatusLog log : logs) {
                historyTableModel.addRow(new Object[]{
                    log.getFormattedTimestamp(),
                    log.getStatus().name(),
                    log.getLocation() != null ? log.getLocation() : "-",
                    log.getRemarks() != null ? log.getRemarks() : "-",
                    log.getUpdatedBy() != null ? log.getUpdatedBy() : "System"
                });
            }
        } catch (Exception e) {
            System.err.println("Error fetching status logs: " + e.getMessage());
        }
    }

    private void clearView() {
        txtTrackingNumber.setText("");
        currentShipment = null;
        btnUpdateStatus.setEnabled(false);

        lblStatusBadge.setText("Awaiting Tracking #");
        lblTrackingId.setText("Tracking ID: -");
        lblServiceType.setText("Service: -");
        lblCurrentLocation.setText("Location: -");
        lblEstimatedDelivery.setText("Estimated Delivery: -");

        lblSenderName.setText("Name: -");
        lblSenderPhone.setText("Phone: -");
        lblSenderAddress.setText("Address: -");

        lblReceiverName.setText("Name: -");
        lblReceiverPhone.setText("Phone: -");
        lblReceiverAddress.setText("Address: -");

        lblParcelDesc.setText("Description: -");
        lblParcelWeight.setText("Weight: -");
        lblParcelDeclared.setText("Declared Value: -");

        stepperPanel.reset();
        historyTableModel.setRowCount(0);
    }

    /**
     * User action: Opens status dialog and delegates update to Controller.
     */
    private void openUpdateStatusDialog() {
        if (currentShipment == null) return;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Update Shipment Status", true);
        dialog.setLayout(new BorderLayout(12, 16));
        dialog.getContentPane().setBackground(UITheme.COLOR_BG_APP);

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 12));
        form.setBorder(new EmptyBorder(16, 20, 16, 20));
        form.setOpaque(false);

        JComboBox<ShipmentStatus> cmbStatus = new JComboBox<>(ShipmentStatus.values());
        cmbStatus.setSelectedItem(currentShipment.getStatus());

        JTextField txtLoc = new JTextField(currentShipment.getCurrentLocation() != null ? currentShipment.getCurrentLocation() : "");
        JTextField txtRemarks = new JTextField();
        JTextField txtUpdatedBy = new JTextField("Agent");

        form.add(new JLabel("New Status:"));
        form.add(cmbStatus);
        form.add(new JLabel("Current Location:"));
        form.add(txtLoc);
        form.add(new JLabel("Remarks / Note:"));
        form.add(txtRemarks);
        form.add(new JLabel("Updated By:"));
        form.add(txtUpdatedBy);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnPanel.setOpaque(false);
        JButton btnSave = UITheme.createPrimaryButton("Save Status Update");
        JButton btnCancel = UITheme.createSecondaryButton("Cancel");

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSave.addActionListener(e -> {
            ShipmentStatus selected = (ShipmentStatus) cmbStatus.getSelectedItem();
            String loc = txtLoc.getText().trim();
            String rem = txtRemarks.getText().trim();
            String by  = txtUpdatedBy.getText().trim();

            try {
                boolean ok = controller.updateShipmentStatus(currentShipment.getTrackingNumber(), selected, loc, rem, by);
                if (ok) {
                    JOptionPane.showMessageDialog(dialog,
                        "Status successfully updated to: " + selected.getDisplayName(),
                        "Update Confirmed",
                        JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();

                    // Reload this shipment view
                    loadTracking(currentShipment.getTrackingNumber());

                    if (onDataChanged != null) {
                        onDataChanged.run();
                    }
                }
            } catch (IllegalStateException | IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "State Validation Warning", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ── Status Stepper Component ───────────────────────────────────────────────

    private static class StatusStepperPanel extends JPanel {
        private final String[] steps = {"Dispatched", "In Transit", "Out for Delivery", "Delivered"};
        private int currentStepIndex = -1;
        private boolean isReturnedOrFailed = false;

        public StatusStepperPanel() {
            setPreferredSize(new Dimension(800, 75));
            setBackground(UITheme.COLOR_CARD_BG);
            setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(10, 30, 10, 30)
            ));
        }

        public void setStatus(ShipmentStatus status) {
            isReturnedOrFailed = (status == ShipmentStatus.FAILED_DELIVERY || status == ShipmentStatus.RETURNED);
            switch (status) {
                case DISPATCHED       -> currentStepIndex = 0;
                case IN_TRANSIT       -> currentStepIndex = 1;
                case OUT_FOR_DELIVERY -> currentStepIndex = 2;
                case DELIVERED        -> currentStepIndex = 3;
                case ON_HOLD          -> currentStepIndex = 1;
                default               -> currentStepIndex = 1;
            }
            repaint();
        }

        public void reset() {
            currentStepIndex = -1;
            isReturnedOrFailed = false;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth() - 80;
            int startX = 40;
            int stepGap = width / (steps.length - 1);
            int circleY = 22;
            int circleRadius = 14;

            g2.setStroke(new BasicStroke(4));
            g2.setColor(UITheme.COLOR_BORDER);
            g2.drawLine(startX, circleY, startX + width, circleY);

            if (currentStepIndex >= 0) {
                int progressX = startX + (currentStepIndex * stepGap);
                g2.setColor(isReturnedOrFailed ? new Color(239, 68, 68) : UITheme.COLOR_ACCENT);
                g2.drawLine(startX, circleY, progressX, circleY);
            }

            for (int i = 0; i < steps.length; i++) {
                int cx = startX + (i * stepGap);
                boolean isCompleted = (i < currentStepIndex);
                boolean isCurrent   = (i == currentStepIndex);

                if (isCurrent) {
                    g2.setColor(isReturnedOrFailed ? new Color(239, 68, 68) : UITheme.COLOR_ACCENT);
                    g2.fillOval(cx - circleRadius, circleY - circleRadius, circleRadius * 2, circleRadius * 2);
                    g2.setColor(Color.WHITE);
                    g2.fillOval(cx - circleRadius + 4, circleY - circleRadius + 4, (circleRadius - 4) * 2, (circleRadius - 4) * 2);
                    g2.setColor(isReturnedOrFailed ? new Color(239, 68, 68) : UITheme.COLOR_ACCENT);
                    g2.fillOval(cx - 5, circleY - 5, 10, 10);
                } else if (isCompleted) {
                    g2.setColor(UITheme.COLOR_DELIVERED_FG);
                    g2.fillOval(cx - circleRadius, circleY - circleRadius, circleRadius * 2, circleRadius * 2);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    g2.drawString("✓", cx - 4, circleY + 4);
                } else {
                    g2.setColor(Color.WHITE);
                    g2.fillOval(cx - circleRadius, circleY - circleRadius, circleRadius * 2, circleRadius * 2);
                    g2.setColor(UITheme.COLOR_BORDER);
                    g2.setStroke(new BasicStroke(2));
                    g2.drawOval(cx - circleRadius, circleY - circleRadius, circleRadius * 2, circleRadius * 2);
                }

                g2.setFont(isCurrent ? UITheme.FONT_BOLD : UITheme.FONT_BODY);
                g2.setColor(isCurrent ? UITheme.COLOR_TEXT_MAIN : UITheme.COLOR_TEXT_MUTED);
                FontMetrics fm = g2.getFontMetrics();
                int labelWidth = fm.stringWidth(steps[i]);
                g2.drawString(steps[i], cx - (labelWidth / 2), circleY + 30);
            }

            g2.dispose();
        }
    }
}
