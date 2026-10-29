package com.courier.view;

import com.courier.controller.ShipmentController;
import com.courier.model.Person;
import com.courier.model.Shipment;
import com.courier.util.TrackingNumberGenerator;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * View component (Java Swing) for capturing new shipment dispatches in MVC architecture.
 * Delegates data validation, persistence, and business logic to ShipmentController.
 */
public class ShipmentForm extends JPanel {

    private final ShipmentController controller;
    private final Runnable onShipmentSaved;

    // Sender Inputs
    private JTextField txtSenderName;
    private JTextField txtSenderPhone;
    private JTextField txtSenderEmail;
    private JTextField txtSenderAddress;
    private JTextField txtSenderCity;
    private JTextField txtSenderState;
    private JTextField txtSenderPincode;

    // Receiver Inputs
    private JTextField txtReceiverName;
    private JTextField txtReceiverPhone;
    private JTextField txtReceiverEmail;
    private JTextField txtReceiverAddress;
    private JTextField txtReceiverCity;
    private JTextField txtReceiverState;
    private JTextField txtReceiverPincode;

    // Parcel Inputs
    private JTextField txtParcelDesc;
    private JComboBox<String> cmbCategory;
    private JTextField txtWeight;
    private JTextField txtDeclaredValue;
    private JCheckBox chkFragile;

    // Service Inputs
    private JComboBox<String> cmbServiceType;
    private JTextField txtOriginHub;
    private JLabel lblEtaPreview;
    private JTextField txtTrackingNumberPreview;

    private static final String[] CATEGORIES = {
        "Electronics", "Documents", "Clothing", "Fragile Items",
        "Food", "Medicine", "Furniture", "Other"
    };

    private static final String[] SERVICE_TYPES = {
        "Standard", "Express", "Same-Day", "Overnight", "International"
    };

    public ShipmentForm(ShipmentController controller, Runnable onShipmentSaved) {
        this.controller = controller;
        this.onShipmentSaved = onShipmentSaved;

        setLayout(new BorderLayout());
        setBackground(UITheme.COLOR_BG_APP);
        setBorder(new EmptyBorder(16, 24, 16, 24));

        initComponents();
        updateEtaPreview();
        generateNewTrackingNumberPreview();
    }

    private void initComponents() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Dispatch New Shipment");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.COLOR_PRIMARY);

        JLabel lblSub = new JLabel("Fill in sender, receiver, parcel details, and dispatch through Controller");
        lblSub.setFont(UITheme.FONT_BODY);
        lblSub.setForeground(UITheme.COLOR_TEXT_MUTED);

        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSub, BorderLayout.SOUTH);
        contentPanel.add(headerPanel);
        contentPanel.add(Box.createVerticalStrut(14));

        // Two-column grid for Sender and Receiver
        JPanel partiesGrid = new JPanel(new GridLayout(1, 2, 16, 0));
        partiesGrid.setOpaque(false);
        partiesGrid.add(createSenderSection());
        partiesGrid.add(createReceiverSection());
        contentPanel.add(partiesGrid);
        contentPanel.add(Box.createVerticalStrut(14));

        // Parcel details
        contentPanel.add(createParcelSection());
        contentPanel.add(Box.createVerticalStrut(14));

        // Service & tracking assignment
        contentPanel.add(createServiceSection());
        contentPanel.add(Box.createVerticalStrut(16));

        // Actions
        JPanel actionsBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actionsBar.setOpaque(false);

        JButton btnDemoData = UITheme.createSecondaryButton("Fill Sample Data");
        JButton btnClear = UITheme.createSecondaryButton("Clear Form");
        JButton btnSave = UITheme.createPrimaryButton("Dispatch & Save Shipment");

        btnDemoData.addActionListener(e -> fillSampleData());
        btnClear.addActionListener(e -> clearForm());
        btnSave.addActionListener(e -> handleSaveShipment());

        actionsBar.add(btnDemoData);
        actionsBar.add(btnClear);
        actionsBar.add(btnSave);
        contentPanel.add(actionsBar);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createSenderSection() {
        JPanel panel = createSectionCard("1. Sender Information (Origin)");
        panel.setLayout(new GridLayout(7, 2, 8, 8));

        txtSenderName    = createTextField("e.g. Ananya Roy");
        txtSenderPhone   = createTextField("e.g. +91 98200 12345");
        txtSenderEmail   = createTextField("e.g. ananya@mail.com");
        txtSenderAddress = createTextField("e.g. 104 Marine Drive");
        txtSenderCity    = createTextField("e.g. Mumbai");
        txtSenderState   = createTextField("e.g. Maharashtra");
        txtSenderPincode = createTextField("e.g. 400020");

        panel.add(new JLabel("Full Name *:"));     panel.add(txtSenderName);
        panel.add(new JLabel("Phone Number *:")); panel.add(txtSenderPhone);
        panel.add(new JLabel("Email Address:"));  panel.add(txtSenderEmail);
        panel.add(new JLabel("Street Address *:")); panel.add(txtSenderAddress);
        panel.add(new JLabel("City:"));           panel.add(txtSenderCity);
        panel.add(new JLabel("State:"));          panel.add(txtSenderState);
        panel.add(new JLabel("Pincode:"));        panel.add(txtSenderPincode);
        return panel;
    }

    private JPanel createReceiverSection() {
        JPanel panel = createSectionCard("2. Receiver Information (Destination)");
        panel.setLayout(new GridLayout(7, 2, 8, 8));

        txtReceiverName    = createTextField("e.g. Rajesh Kumar");
        txtReceiverPhone   = createTextField("e.g. +91 98450 67890");
        txtReceiverEmail   = createTextField("e.g. rajesh@company.com");
        txtReceiverAddress = createTextField("e.g. 45 Indiranagar, 100ft Road");
        txtReceiverCity    = createTextField("e.g. Bengaluru");
        txtReceiverState   = createTextField("e.g. Karnataka");
        txtReceiverPincode = createTextField("e.g. 560038");

        panel.add(new JLabel("Full Name *:"));     panel.add(txtReceiverName);
        panel.add(new JLabel("Phone Number *:")); panel.add(txtReceiverPhone);
        panel.add(new JLabel("Email Address:"));  panel.add(txtReceiverEmail);
        panel.add(new JLabel("Street Address *:")); panel.add(txtReceiverAddress);
        panel.add(new JLabel("City:"));           panel.add(txtReceiverCity);
        panel.add(new JLabel("State:"));          panel.add(txtReceiverState);
        panel.add(new JLabel("Pincode:"));        panel.add(txtReceiverPincode);
        return panel;
    }

    private JPanel createParcelSection() {
        JPanel panel = createSectionCard("3. Parcel Specifications");
        panel.setLayout(new GridLayout(3, 4, 12, 10));

        txtParcelDesc     = createTextField("e.g. Confidential Legal Documents");
        cmbCategory       = new JComboBox<>(CATEGORIES);
        txtWeight         = createTextField("1.5");
        txtDeclaredValue  = createTextField("1500.0");
        chkFragile        = new JCheckBox("Mark as Fragile / Handle With Care");
        chkFragile.setOpaque(false);

        panel.add(new JLabel("Description *:"));
        panel.add(txtParcelDesc);
        panel.add(new JLabel("Category:"));
        panel.add(cmbCategory);

        panel.add(new JLabel("Weight (kg) *:"));
        panel.add(txtWeight);
        panel.add(new JLabel("Declared Value (Rs.):"));
        panel.add(txtDeclaredValue);

        panel.add(new JLabel("Special Handling:"));
        panel.add(chkFragile);
        panel.add(new JLabel(""));
        panel.add(new JLabel(""));
        return panel;
    }

    private JPanel createServiceSection() {
        JPanel panel = createSectionCard("4. Service & Tracking Assignment");
        panel.setLayout(new GridLayout(2, 4, 12, 10));

        cmbServiceType = new JComboBox<>(SERVICE_TYPES);
        cmbServiceType.addActionListener(e -> updateEtaPreview());

        txtOriginHub = createTextField("Central Sorting Hub");

        lblEtaPreview = new JLabel("ETA: Calculating...");
        lblEtaPreview.setFont(UITheme.FONT_BOLD);
        lblEtaPreview.setForeground(UITheme.COLOR_ACCENT);

        txtTrackingNumberPreview = createTextField("");
        txtTrackingNumberPreview.setEditable(false);
        txtTrackingNumberPreview.setBackground(new Color(241, 245, 249));

        panel.add(new JLabel("Service Type:"));
        panel.add(cmbServiceType);
        panel.add(new JLabel("Origin Facility:"));
        panel.add(txtOriginHub);

        panel.add(new JLabel("Estimated Delivery:"));
        panel.add(lblEtaPreview);
        panel.add(new JLabel("Assigned Tracking ID:"));
        panel.add(txtTrackingNumberPreview);

        return panel;
    }

    private JPanel createSectionCard(String title) {
        JPanel panel = UITheme.createCardPanel();
        panel.setBorder(new CompoundBorder(
            new TitledBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                title,
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                UITheme.FONT_SUBTITLE,
                UITheme.COLOR_PRIMARY
            ),
            new EmptyBorder(8, 12, 8, 12)
        ));
        return panel;
    }

    private JTextField createTextField(String placeholder) {
        JTextField tf = new JTextField();
        tf.setFont(UITheme.FONT_BODY);
        tf.setBorder(new CompoundBorder(
            new LineBorder(UITheme.COLOR_BORDER, 1, true),
            new EmptyBorder(6, 8, 6, 8)
        ));
        tf.setToolTipText(placeholder);
        return tf;
    }

    private void updateEtaPreview() {
        String serviceType = (String) cmbServiceType.getSelectedItem();
        LocalDateTime eta = controller.calculateEstimatedDelivery(serviceType);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");
        lblEtaPreview.setText(eta.format(fmt));
    }

    private void generateNewTrackingNumberPreview() {
        txtTrackingNumberPreview.setText(TrackingNumberGenerator.generate());
    }

    /**
     * User action triggered: View collects data into Model and delegates to Controller.
     */
    private void handleSaveShipment() {
        Person sender = new Person(
            txtSenderName.getText().trim(),
            txtSenderAddress.getText().trim(),
            txtSenderCity.getText().trim(),
            txtSenderState.getText().trim(),
            txtSenderPincode.getText().trim(),
            txtSenderPhone.getText().trim(),
            txtSenderEmail.getText().trim()
        );

        Person receiver = new Person(
            txtReceiverName.getText().trim(),
            txtReceiverAddress.getText().trim(),
            txtReceiverCity.getText().trim(),
            txtReceiverState.getText().trim(),
            txtReceiverPincode.getText().trim(),
            txtReceiverPhone.getText().trim(),
            txtReceiverEmail.getText().trim()
        );

        Shipment shipment = new Shipment();
        shipment.setTrackingNumber(txtTrackingNumberPreview.getText().trim());
        shipment.setSender(sender);
        shipment.setReceiver(receiver);
        shipment.setParcelDescription(txtParcelDesc.getText().trim());
        shipment.setParcelCategory((String) cmbCategory.getSelectedItem());
        shipment.setFragile(chkFragile.isSelected());
        shipment.setServiceType((String) cmbServiceType.getSelectedItem());
        shipment.setCurrentLocation(txtOriginHub.getText().trim());

        try {
            double wt = Double.parseDouble(txtWeight.getText().trim());
            shipment.setWeightKg(wt);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "Please enter a valid numeric weight (e.g. 2.5).",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            txtWeight.requestFocus();
            return;
        }

        try {
            String valStr = txtDeclaredValue.getText().trim();
            double val = valStr.isEmpty() ? 0.0 : Double.parseDouble(valStr);
            shipment.setDeclaredValue(val);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "Please enter a valid declared value.",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            txtDeclaredValue.requestFocus();
            return;
        }

        // Delegate to Controller
        try {
            Shipment saved = controller.dispatchShipment(shipment);
            JOptionPane.showMessageDialog(this,
                "Shipment Dispatched Successfully!\n\n" +
                "Tracking ID: " + saved.getTrackingNumber() + "\n" +
                "Estimated Delivery: " + saved.getFormattedEstimatedDelivery() + "\n" +
                "Service: " + saved.getServiceType(),
                "Dispatch Confirmed",
                JOptionPane.INFORMATION_MESSAGE);

            clearForm();

            if (onShipmentSaved != null) {
                onShipmentSaved.run();
            }
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this,
                e.getMessage(),
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Database error while saving shipment:\n" + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void fillSampleData() {
        txtSenderName.setText("TechLogix Solutions Ltd.");
        txtSenderPhone.setText("+91 98200 44332");
        txtSenderEmail.setText("logistics@techlogix.com");
        txtSenderAddress.setText("Tower B, Cyber City Phase 2");
        txtSenderCity.setText("Gurugram");
        txtSenderState.setText("Haryana");
        txtSenderPincode.setText("122002");

        txtReceiverName.setText("Naveen Chandran");
        txtReceiverPhone.setText("+91 98401 55667");
        txtReceiverEmail.setText("n.chandran@innovate.in");
        txtReceiverAddress.setText("No. 18 Anna Salai, Guindy");
        txtReceiverCity.setText("Chennai");
        txtReceiverState.setText("Tamil Nadu");
        txtReceiverPincode.setText("600032");

        txtParcelDesc.setText("Precision IoT Sensor Modules & Controllers");
        cmbCategory.setSelectedItem("Electronics");
        txtWeight.setText("2.8");
        txtDeclaredValue.setText("45000.0");
        chkFragile.setSelected(true);

        cmbServiceType.setSelectedItem("Express");
        txtOriginHub.setText("Delhi NCR Central Air Freight Hub");

        updateEtaPreview();
        generateNewTrackingNumberPreview();
    }

    private void clearForm() {
        txtSenderName.setText("");
        txtSenderPhone.setText("");
        txtSenderEmail.setText("");
        txtSenderAddress.setText("");
        txtSenderCity.setText("");
        txtSenderState.setText("");
        txtSenderPincode.setText("");

        txtReceiverName.setText("");
        txtReceiverPhone.setText("");
        txtReceiverEmail.setText("");
        txtReceiverAddress.setText("");
        txtReceiverCity.setText("");
        txtReceiverState.setText("");
        txtReceiverPincode.setText("");

        txtParcelDesc.setText("");
        cmbCategory.setSelectedIndex(0);
        txtWeight.setText("");
        txtDeclaredValue.setText("");
        chkFragile.setSelected(false);

        cmbServiceType.setSelectedIndex(0);
        txtOriginHub.setText("Central Hub");

        updateEtaPreview();
        generateNewTrackingNumberPreview();
    }
}
