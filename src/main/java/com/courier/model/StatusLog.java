package com.courier.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StatusLog {
    private int id;
    private String trackingNumber;
    private ShipmentStatus status;
    private String location;
    private String remarks;
    private LocalDateTime timestamp;
    private String updatedBy;

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

    public StatusLog() {}

    public StatusLog(String trackingNumber, ShipmentStatus status,
                     String location, String remarks, String updatedBy) {
        this.trackingNumber = trackingNumber;
        this.status         = status;
        this.location       = location;
        this.remarks        = remarks;
        this.updatedBy      = updatedBy;
        this.timestamp      = LocalDateTime.now();
    }

    public int getId()                  { return id; }
    public String getTrackingNumber()   { return trackingNumber; }
    public ShipmentStatus getStatus()   { return status; }
    public String getLocation()         { return location; }
    public String getRemarks()          { return remarks; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getUpdatedBy()        { return updatedBy; }

    public void setId(int v)                           { this.id = v; }
    public void setTrackingNumber(String v)            { this.trackingNumber = v; }
    public void setStatus(ShipmentStatus v)            { this.status = v; }
    public void setLocation(String v)                  { this.location = v; }
    public void setRemarks(String v)                   { this.remarks = v; }
    public void setTimestamp(LocalDateTime v)          { this.timestamp = v; }
    public void setUpdatedBy(String v)                 { this.updatedBy = v; }

    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(FMT) : "N/A";
    }

    @Override
    public String toString() {
        return "[" + getFormattedTimestamp() + "] " + status.getDisplayName()
             + " @ " + location + " | " + remarks;
    }
}
