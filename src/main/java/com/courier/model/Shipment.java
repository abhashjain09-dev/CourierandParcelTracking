package com.courier.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Shipment {
    private int id;
    private String trackingNumber;
    private Person sender;
    private Person receiver;
    private ShipmentStatus status;
    private String parcelDescription;
    private double weightKg;
    private String parcelCategory;
    private double declaredValue;
    private LocalDateTime dispatchDate;
    private LocalDateTime estimatedDelivery;
    private LocalDateTime lastUpdated;
    private String currentLocation;
    private String remarks;
    private boolean isFragile;
    private String serviceType;

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    public Shipment() {
        this.sender   = new Person();
        this.receiver = new Person();
        this.dispatchDate  = LocalDateTime.now();
        this.lastUpdated   = LocalDateTime.now();
        this.status        = ShipmentStatus.DISPATCHED;
    }

    public int getId()                          { return id; }
    public String getTrackingNumber()           { return trackingNumber; }
    public Person getSender()                   { return sender; }
    public Person getReceiver()                 { return receiver; }
    public ShipmentStatus getStatus()           { return status; }
    public String getParcelDescription()        { return parcelDescription; }
    public double getWeightKg()                 { return weightKg; }
    public String getParcelCategory()           { return parcelCategory; }
    public double getDeclaredValue()            { return declaredValue; }
    public LocalDateTime getDispatchDate()      { return dispatchDate; }
    public LocalDateTime getEstimatedDelivery() { return estimatedDelivery; }
    public LocalDateTime getLastUpdated()       { return lastUpdated; }
    public String getCurrentLocation()          { return currentLocation; }
    public String getRemarks()                  { return remarks; }
    public boolean isFragile()                  { return isFragile; }
    public String getServiceType()              { return serviceType; }

    public void setId(int v)                              { this.id = v; }
    public void setTrackingNumber(String v)               { this.trackingNumber = v; }
    public void setSender(Person v)                       { this.sender = v; }
    public void setReceiver(Person v)                     { this.receiver = v; }
    public void setStatus(ShipmentStatus v)               { this.status = v; this.lastUpdated = LocalDateTime.now(); }
    public void setParcelDescription(String v)            { this.parcelDescription = v; }
    public void setWeightKg(double v)                     { this.weightKg = v; }
    public void setParcelCategory(String v)               { this.parcelCategory = v; }
    public void setDeclaredValue(double v)                { this.declaredValue = v; }
    public void setDispatchDate(LocalDateTime v)          { this.dispatchDate = v; }
    public void setEstimatedDelivery(LocalDateTime v)     { this.estimatedDelivery = v; }
    public void setLastUpdated(LocalDateTime v)           { this.lastUpdated = v; }
    public void setCurrentLocation(String v)              { this.currentLocation = v; }
    public void setRemarks(String v)                      { this.remarks = v; }
    public void setFragile(boolean v)                     { this.isFragile = v; }
    public void setServiceType(String v)                  { this.serviceType = v; }

    public String getFormattedDispatchDate()      { return dispatchDate      != null ? dispatchDate.format(FMT)      : "N/A"; }
    public String getFormattedEstimatedDelivery() { return estimatedDelivery != null ? estimatedDelivery.format(FMT) : "N/A"; }
    public String getFormattedLastUpdated()       { return lastUpdated       != null ? lastUpdated.format(FMT)       : "N/A"; }

    @Override
    public String toString() {
        return "Tracking#: " + trackingNumber + " | Status: " + status.getDisplayName()
             + " | From: " + sender.getName() + " -> To: " + receiver.getName();
    }
}
