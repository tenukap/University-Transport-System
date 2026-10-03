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

    @Column(name = "bus_id")
    private Long busNo;

    @Column(name = "driver_user_id")
    private Integer driverUserId;

    @Column(name = "Location_Coordinates", nullable = false, length = 255)
    private String locationCoordinates;

    @Column(name = "Severity_Level", nullable = false, length = 50)
    private String severityLevel;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "Timestamp")
    private LocalDateTime timestamp;

    @Column(name = "Status", length = 50)
    private String status = "Pending";

    @Column(name = "trip_id")
    private Integer tripId;

    public CrashIncident() {
    }

    public Integer getIncidentId() { return incidentId; }
    public void setIncidentId(Integer incidentId) { this.incidentId = incidentId; }

    public Long getBusNo() { return busNo; }
    public void setBusNo(Long busNo) { this.busNo = busNo; }

    public Integer getDriverUserId() { return driverUserId; }
    public void setDriverUserId(Integer driverUserId) { this.driverUserId = driverUserId; }

    public String getLocationCoordinates() { return locationCoordinates; }
    public void setLocationCoordinates(String locationCoordinates) { this.locationCoordinates = locationCoordinates; }

    public String getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(String severityLevel) { this.severityLevel = severityLevel; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getTripId() { return tripId; }
    public void setTripId(Integer tripId) { this.tripId = tripId; }
}
