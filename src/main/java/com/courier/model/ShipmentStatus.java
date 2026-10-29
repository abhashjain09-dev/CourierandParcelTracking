package com.courier.model;

public enum ShipmentStatus {
    DISPATCHED("Dispatched", "Shipment has been dispatched from origin"),
    IN_TRANSIT("In-Transit", "Shipment is currently in transit"),
    OUT_FOR_DELIVERY("Out for Delivery", "Shipment is out for delivery"),
    DELIVERED("Delivered", "Shipment has been delivered successfully"),
    FAILED_DELIVERY("Failed Delivery", "Delivery attempt was unsuccessful"),
    RETURNED("Returned", "Shipment has been returned to sender"),
    ON_HOLD("On Hold", "Shipment is on hold");

    private final String displayName;
    private final String description;

    ShipmentStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    public static ShipmentStatus fromString(String status) {
        for (ShipmentStatus s : values()) {
            if (s.name().equalsIgnoreCase(status) || s.displayName.equalsIgnoreCase(status))
                return s;
        }
        return IN_TRANSIT;
    }

    @Override
    public String toString() { return displayName; }
}
