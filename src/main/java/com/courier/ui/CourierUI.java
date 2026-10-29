package com.courier.ui;

import com.courier.model.*;
import com.courier.service.ShipmentService;
import com.courier.util.ConsoleHelper;

import java.sql.SQLException;
import java.util.List;

import static com.courier.util.ConsoleHelper.*;

/**
 * Console-based user interface for the Courier Tracking System.
 * Provides a full menu-driven interface covering all CRUD operations.
 */
public class CourierUI {

    private final ShipmentService service;
    private boolean running = true;

    private static final String[] SERVICE_TYPES = {
        "Standard (5 days)", "Express (2 days)", "Same-Day (8 hrs)"
    };
    private static final String[] PARCEL_CATEGORIES = {
        "Electronics", "Clothing", "Documents", "Food",
        "Fragile Items", "Furniture", "Medicine", "Other"
    };

    public CourierUI(ShipmentService service) {
        this.service = service;
    }

    public void start() {
        printBanner();
        while (running) showMainMenu();
        printInfo("Thank you for using Courier Tracking System. Goodbye!");
    }

    // ─── Banner ───────────────────────────────────────────────────────────────

    private void printBanner() {
        System.out.println(PURPLE);
        System.out.println("  ██████╗ ████████╗███████╗");
        System.out.println(" ██╔════╝    ██╔══╝██╔════╝");
        System.out.println(" ██║         ██║   ███████╗");
        System.out.println(" ██║         ██║   ╚════██║");
        System.out.println(" ╚██████╗    ██║   ███████║");
        System.out.println("  ╚═════╝    ╚═╝   ╚══════╝");
        System.out.println(RESET);
        System.out.println(BOLD + YELLOW + "   COURIER & PARCEL TRACKING SYSTEM  v1.0" + RESET);
        System.out.println(CYAN  + "   Powered by SQLite  |  Built with Java 21" + RESET);
        printSeparator();
    }

    // ─── Main Menu ────────────────────────────────────────────────────────────

    private void showMainMenu() {
        System.out.println();
        printSeparator();
        System.out.println(BOLD + "                   MAIN MENU" + RESET);
        printSeparator();
        System.out.println(GREEN  + "  [1]" + RESET + "  Dispatch New Shipment");
        System.out.println(GREEN  + "  [2]" + RESET + "  Track Shipment (by Tracking #)");
        System.out.println(GREEN  + "  [3]" + RESET + "  Update Shipment Status");
        System.out.println(GREEN  + "  [4]" + RESET + "  View All Shipments");
        System.out.println(GREEN  + "  [5]" + RESET + "  Filter Shipments by Status");
        System.out.println(GREEN  + "  [6]" + RESET + "  Search Shipments");
        System.out.println(GREEN  + "  [7]" + RESET + "  View Tracking History / Audit Log");
        System.out.println(GREEN  + "  [8]" + RESET + "  Dashboard & Statistics");
        System.out.println(GREEN  + "  [9]" + RESET + "  Delete Shipment");
        System.out.println(RED    + "  [0]" + RESET + "  Exit");
        printSeparator();

        int choice = readInt("  Enter your choice: ");
        switch (choice) {
            case 1 -> dispatchShipmentMenu();
            case 2 -> trackShipmentMenu();
            case 3 -> updateStatusMenu();
            case 4 -> viewAllShipments();
            case 5 -> filterByStatusMenu();
            case 6 -> searchMenu();
            case 7 -> viewHistoryMenu();
            case 8 -> showDashboard();
            case 9 -> deleteShipmentMenu();
            case 0 -> running = false;
            default -> printError("Invalid choice. Please select 0-9.");
        }
    }

    // ─── 1. Dispatch Shipment ─────────────────────────────────────────────────

    private void dispatchShipmentMenu() {
        printHeader("DISPATCH NEW SHIPMENT");
        Shipment shipment = new Shipment();

        printSubHeader("Sender Details");
        Person sender = new Person();
        sender.setName(readLine("  Sender Name          : "));
        sender.setPhone(readLine("  Sender Phone         : "));
        sender.setEmail(readLineOptional("  Sender Email"));
        sender.setAddress(readLine("  Sender Address       : "));
        sender.setCity(readLine("  Sender City          : "));
        sender.setState(readLine("  Sender State         : "));
        sender.setPincode(readLine("  Sender Pincode       : "));
        shipment.setSender(sender);

        printSubHeader("Receiver Details");
        Person receiver = new Person();
        receiver.setName(readLine("  Receiver Name        : "));
        receiver.setPhone(readLine("  Receiver Phone       : "));
        receiver.setEmail(readLineOptional("  Receiver Email"));
        receiver.setAddress(readLine("  Receiver Address     : "));
        receiver.setCity(readLine("  Receiver City        : "));
        receiver.setState(readLine("  Receiver State       : "));
        receiver.setPincode(readLine("  Receiver Pincode     : "));
        shipment.setReceiver(receiver);

        printSubHeader("Parcel Details");
        shipment.setParcelDescription(readLine("  Description          : "));

        System.out.println(CYAN + "  Parcel Category:" + RESET);
        for (int i = 0; i < PARCEL_CATEGORIES.length; i++)
            System.out.printf("  [%d] %s%n", i + 1, PARCEL_CATEGORIES[i]);
        int cat = readInt("  Select Category      : ");
        shipment.setParcelCategory((cat >= 1 && cat <= PARCEL_CATEGORIES.length)
            ? PARCEL_CATEGORIES[cat - 1] : "Other");

        shipment.setWeightKg(readDouble("  Weight (kg)          : "));
        shipment.setDeclaredValue(readDouble("  Declared Value (Rs.) : "));
        shipment.setFragile(readBoolean("  Is Fragile?"));

        printSubHeader("Service Type");
        for (int i = 0; i < SERVICE_TYPES.length; i++)
            System.out.printf("  [%d] %s%n", i + 1, SERVICE_TYPES[i]);
        int svc = readInt("  Select Service       : ");
        shipment.setServiceType(switch (svc) {
            case 2 -> "Express";
            case 3 -> "Same-Day";
            default -> "Standard";
        });
        shipment.setCurrentLocation(readLine("  Origin Hub/Location  : "));

        try {
            Shipment saved = service.dispatchShipment(shipment);
            System.out.println();
            printSuccess("Shipment dispatched successfully!");
            printSeparator();
            printRow("Tracking Number",    saved.getTrackingNumber());
            printRow("Status",             saved.getStatus().getDisplayName());
            printRow("Service Type",       saved.getServiceType());
            printRow("Dispatch Date",      saved.getFormattedDispatchDate());
            printRow("Estimated Delivery", saved.getFormattedEstimatedDelivery());
            printSeparator();
        } catch (SQLException e) {
            printError("Database error: " + e.getMessage());
        }
        pressEnterToContinue();
    }

    // ─── 2. Track Shipment ────────────────────────────────────────────────────

    private void trackShipmentMenu() {
        printHeader("TRACK SHIPMENT");
        String tn = readLine("  Enter Tracking Number: ");
        try {
            Shipment s = service.trackShipment(tn);
            if (s == null) printError("No shipment found: " + tn);
            else           displayShipmentDetails(s);
        } catch (SQLException e) {
            printError("Database error: " + e.getMessage());
        }
        pressEnterToContinue();
    }

    private void displayShipmentDetails(Shipment s) {
        printSeparator();
        System.out.println(BOLD + YELLOW + "  SHIPMENT DETAILS" + RESET);
        printSeparator();
        printRow("Tracking Number",    s.getTrackingNumber());
        printRow("Status",             statusBadge(s.getStatus()));
        printRow("Service Type",       s.getServiceType());
        printRow("Current Location",   nvl(s.getCurrentLocation()));
        printRow("Dispatch Date",      s.getFormattedDispatchDate());
        printRow("Estimated Delivery", s.getFormattedEstimatedDelivery());
        printRow("Last Updated",       s.getFormattedLastUpdated());

        printSubHeader("Parcel Information");
        printRow("Description",   nvl(s.getParcelDescription()));
        printRow("Category",      nvl(s.getParcelCategory()));
        printRow("Weight",        s.getWeightKg() + " kg");
        printRow("Declared Value","Rs. " + String.format("%.2f", s.getDeclaredValue()));
        printRow("Fragile",       s.isFragile() ? "Yes" : "No");
        printRow("Remarks",       nvl(s.getRemarks()));

        printSubHeader("Sender");
        printRow("Name",    s.getSender().getName());
        printRow("Phone",   s.getSender().getPhone());
        printRow("Email",   nvl(s.getSender().getEmail()));
        printRow("Address", s.getSender().getFullAddress());

        printSubHeader("Receiver");
        printRow("Name",    s.getReceiver().getName());
        printRow("Phone",   s.getReceiver().getPhone());
        printRow("Email",   nvl(s.getReceiver().getEmail()));
        printRow("Address", s.getReceiver().getFullAddress());
        printSeparator();
    }

    private String statusBadge(ShipmentStatus status) {
        return switch (status) {
            case DISPATCHED       -> BLUE   + "[ DISPATCHED ]"       + RESET;
            case IN_TRANSIT       -> YELLOW + "[ IN-TRANSIT ]"       + RESET;
            case OUT_FOR_DELIVERY -> CYAN   + "[ OUT FOR DELIVERY ]" + RESET;
            case DELIVERED        -> GREEN  + "[ DELIVERED ]"        + RESET;
            case FAILED_DELIVERY  -> RED    + "[ FAILED DELIVERY ]"  + RESET;
            case RETURNED         -> PURPLE + "[ RETURNED ]"         + RESET;
            case ON_HOLD          -> YELLOW + "[ ON HOLD ]"          + RESET;
        };
    }

    // ─── 3. Update Status ─────────────────────────────────────────────────────

    private void updateStatusMenu() {
        printHeader("UPDATE SHIPMENT STATUS");
        String tn = readLine("  Tracking Number   : ");
        try {
            Shipment s = service.trackShipment(tn);
            if (s == null) {
                printError("Shipment not found: " + tn);
                pressEnterToContinue(); return;
            }

            System.out.println(BOLD + "  Current Status: " + statusBadge(s.getStatus()) + RESET);
            printSubHeader("Select New Status");

            ShipmentStatus[] all = ShipmentStatus.values();
            for (int i = 0; i < all.length; i++)
                System.out.printf("  [%d] %-22s - %s%n", i + 1, all[i].getDisplayName(), all[i].getDescription());

            int choice = readInt("  Select status     : ");
            if (choice < 1 || choice > all.length) { printError("Invalid choice."); pressEnterToContinue(); return; }

            ShipmentStatus newStatus = all[choice - 1];
            String location  = readLine("  Current Location  : ");
            String remarks   = readLine("  Remarks/Notes     : ");
            String updatedBy = readLine("  Updated By        : ");

            if (service.updateShipmentStatus(tn, newStatus, location, remarks, updatedBy)) {
                printSuccess("Status updated to: " + newStatus.getDisplayName());
            } else {
                printError("Failed to update status.");
            }
        } catch (IllegalStateException e) {
            printWarning(e.getMessage());
        } catch (SQLException e) {
            printError("Database error: " + e.getMessage());
        }
        pressEnterToContinue();
    }

    // ─── 4. View All Shipments ────────────────────────────────────────────────

    private void viewAllShipments() {
        printHeader("ALL SHIPMENTS");
        try {
            List<Shipment> list = service.getAllShipments();
            if (list.isEmpty()) printInfo("No shipments found in the system.");
            else                printShipmentTable(list);
        } catch (SQLException e) { printError("Database error: " + e.getMessage()); }
        pressEnterToContinue();
    }

    private void printShipmentTable(List<Shipment> list) {
        printSeparator();
        System.out.printf("  %-24s %-18s %-16s %-16s %-16s%n",
            BOLD+"Tracking #"+RESET, BOLD+"Sender"+RESET,
            BOLD+"Receiver"+RESET,  BOLD+"Status"+RESET, BOLD+"Dispatched"+RESET);
        printSeparator();
        for (Shipment s : list) {
            String col = switch (s.getStatus()) {
                case DELIVERED        -> GREEN;
                case IN_TRANSIT       -> YELLOW;
                case OUT_FOR_DELIVERY -> CYAN;
                case FAILED_DELIVERY  -> RED;
                default               -> WHITE;
            };
            System.out.printf("  %-24s %-18s %-16s %s%-16s%s %-16s%n",
                s.getTrackingNumber(),
                trunc(s.getSender().getName(),   16),
                trunc(s.getReceiver().getName(), 14),
                col, s.getStatus().getDisplayName(), RESET,
                s.getFormattedDispatchDate());
        }
        printSeparator();
        printInfo("Total: " + list.size() + " shipment(s)");
    }

    // ─── 5. Filter by Status ──────────────────────────────────────────────────

    private void filterByStatusMenu() {
        printHeader("FILTER BY STATUS");
        ShipmentStatus[] all = ShipmentStatus.values();
        for (int i = 0; i < all.length; i++)
            System.out.printf("  [%d] %s%n", i + 1, all[i].getDisplayName());
        int choice = readInt("  Select Status: ");
        if (choice < 1 || choice > all.length) { printError("Invalid."); pressEnterToContinue(); return; }
        try {
            List<Shipment> list = service.getShipmentsByStatus(all[choice - 1]);
            System.out.println(BOLD + "\n  Shipments with status: " + all[choice-1].getDisplayName() + RESET);
            if (list.isEmpty()) printInfo("None found.");
            else                printShipmentTable(list);
        } catch (SQLException e) { printError("Database error: " + e.getMessage()); }
        pressEnterToContinue();
    }

    // ─── 6. Search ────────────────────────────────────────────────────────────

    private void searchMenu() {
        printHeader("SEARCH SHIPMENTS");
        System.out.println("  [1] By Sender Name");
        System.out.println("  [2] By Receiver Name");
        int choice = readInt("  Choose: ");
        try {
            List<Shipment> results;
            if (choice == 1) {
                String n = readLine("  Sender Name (partial): ");
                results = service.searchBySender(n);
                printInfo("Sender search: " + n);
            } else if (choice == 2) {
                String n = readLine("  Receiver Name (partial): ");
                results = service.searchByReceiver(n);
                printInfo("Receiver search: " + n);
            } else {
                printError("Invalid choice."); pressEnterToContinue(); return;
            }
            if (results.isEmpty()) printInfo("No shipments found.");
            else                   printShipmentTable(results);
        } catch (SQLException e) { printError("Database error: " + e.getMessage()); }
        pressEnterToContinue();
    }

    // ─── 7. View History ──────────────────────────────────────────────────────

    private void viewHistoryMenu() {
        printHeader("TRACKING HISTORY / AUDIT LOG");
        String tn = readLine("  Enter Tracking Number: ");
        try {
            Shipment s = service.trackShipment(tn);
            if (s == null) { printError("Not found: " + tn); pressEnterToContinue(); return; }

            System.out.println(BOLD + "\n  History for: " + YELLOW + tn + RESET);
            System.out.println(BOLD + "  " + s.getSender().getName() + " --> " + s.getReceiver().getName() + RESET);

            List<StatusLog> logs = service.getShipmentHistory(tn);
            if (logs.isEmpty()) {
                printInfo("No history records found.");
            } else {
                printSeparator();
                System.out.printf("  %-24s %-20s %-20s %-20s%n",
                    BOLD+"Timestamp"+RESET, BOLD+"Status"+RESET,
                    BOLD+"Location"+RESET,  BOLD+"Remarks"+RESET);
                printSeparator();
                for (StatusLog log : logs) {
                    String col = switch (log.getStatus()) {
                        case DELIVERED        -> GREEN;
                        case IN_TRANSIT       -> YELLOW;
                        case OUT_FOR_DELIVERY -> CYAN;
                        case FAILED_DELIVERY  -> RED;
                        default               -> WHITE;
                    };
                    System.out.printf("  %-24s %s%-20s%s %-20s %-20s%n",
                        log.getFormattedTimestamp(),
                        col, log.getStatus().getDisplayName(), RESET,
                        trunc(nvl(log.getLocation()), 18),
                        trunc(nvl(log.getRemarks()),  18));
                }
                printSeparator();
                printInfo("Total events: " + logs.size());
            }
        } catch (SQLException e) { printError("Database error: " + e.getMessage()); }
        pressEnterToContinue();
    }

    // ─── 8. Dashboard ─────────────────────────────────────────────────────────

    private void showDashboard() {
        printHeader("DASHBOARD & STATISTICS");
        try {
            int total       = service.getTotalCount();
            int dispatched  = service.getCountByStatus(ShipmentStatus.DISPATCHED);
            int inTransit   = service.getCountByStatus(ShipmentStatus.IN_TRANSIT);
            int outDel      = service.getCountByStatus(ShipmentStatus.OUT_FOR_DELIVERY);
            int delivered   = service.getCountByStatus(ShipmentStatus.DELIVERED);
            int failed      = service.getCountByStatus(ShipmentStatus.FAILED_DELIVERY);
            int returned    = service.getCountByStatus(ShipmentStatus.RETURNED);
            int onHold      = service.getCountByStatus(ShipmentStatus.ON_HOLD);

            System.out.println();
            printRow("Total Shipments",   String.valueOf(total));
            printRow("Dispatched",        BLUE   + dispatched + RESET);
            printRow("In-Transit",        YELLOW + inTransit  + RESET);
            printRow("Out for Delivery",  CYAN   + outDel     + RESET);
            printRow("Delivered",         GREEN  + delivered  + RESET);
            printRow("Failed Delivery",   RED    + failed     + RESET);
            printRow("Returned",          PURPLE + returned   + RESET);
            printRow("On Hold",           YELLOW + onHold     + RESET);
            printSeparator();
            if (total > 0) {
                double rate = (double) delivered / total * 100;
                printRow("Delivery Success Rate", String.format("%.1f%%", rate));
            }

            printSubHeader("Recent Activity (last 10 updates)");
            List<StatusLog> recent = service.getRecentActivity(10);
            if (recent.isEmpty()) {
                printInfo("No recent activity.");
            } else {
                System.out.printf("  %-24s %-24s %-20s%n",
                    BOLD+"Timestamp"+RESET, BOLD+"Tracking #"+RESET, BOLD+"Status"+RESET);
                printSeparator();
                for (StatusLog log : recent)
                    System.out.printf("  %-24s %-24s %-20s%n",
                        log.getFormattedTimestamp(), log.getTrackingNumber(),
                        log.getStatus().getDisplayName());
            }
        } catch (SQLException e) { printError("Database error: " + e.getMessage()); }
        pressEnterToContinue();
    }

    // ─── 9. Delete Shipment ───────────────────────────────────────────────────

    private void deleteShipmentMenu() {
        printHeader("DELETE SHIPMENT");
        printWarning("This permanently removes the shipment and all its history.");
        String tn = readLine("  Enter Tracking Number: ");
        try {
            Shipment s = service.trackShipment(tn);
            if (s == null) { printError("Not found: " + tn); pressEnterToContinue(); return; }
            System.out.println(BOLD + "  Shipment: " + RESET + s);
            if (readBoolean("  Confirm delete?")) {
                if (service.deleteShipment(tn)) printSuccess("Shipment deleted.");
                else                            printError("Delete failed.");
            } else {
                printInfo("Delete cancelled.");
            }
        } catch (SQLException e) { printError("Database error: " + e.getMessage()); }
        pressEnterToContinue();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String nvl(String s) { return s != null ? s : "N/A"; }
    private String trunc(String s, int max) {
        if (s == null) return "N/A";
        return s.length() > max ? s.substring(0, max - 2) + ".." : s;
    }
}
