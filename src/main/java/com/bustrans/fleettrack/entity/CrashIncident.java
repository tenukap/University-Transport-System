package com.bustrans.fleettrack.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Crash_Incident")
public class CrashIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Incident_ID")
    private Integer incidentId;

    @Column(name = "Bus_No")
    private Integer busNo;

    @Column(name = "Driver_No")
    private Integer driverNo;

    @Column(name = "Location_Coordinates", nullable = false, length = 255)
    private String locationCoordinates;

    @Column(name = "Severity_Level", nullable = false, length = 50)
    private String severityLevel;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "Timestamp")
    private LocalDateTime timestamp;

    @Column(name = "Status", length = 50)
    private String status = "Reported";

    // Default Constructor
    public CrashIncident() {
    }

    // Getters and Setters

    public Integer getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Integer incidentId) {
        this.incidentId = incidentId;
    }

    public Integer getBusNo() {
        return busNo;
    }

    public void setBusNo(Integer busNo) {
        this.busNo = busNo;
    }

    public Integer getDriverNo() {
        return driverNo;
    }

    public void setDriverNo(Integer driverNo) {
        this.driverNo = driverNo;
    }

    public String getLocationCoordinates() {
        return locationCoordinates;
    }

    public void setLocationCoordinates(String locationCoordinates) {
        this.locationCoordinates = locationCoordinates;
    }

    public String getSeverityLevel() {
        return severityLevel;
    }

    public void setSeverityLevel(String severityLevel) {
        this.severityLevel = severityLevel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
