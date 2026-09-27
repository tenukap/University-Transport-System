package com.bustrans.fleettrack.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Location_Update")
public class LocationUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Location_Update_Id")
    private Integer locationUpdateId;

    @Column(name = "Trip_Id", nullable = false)
    private Integer tripId;

    @Column(name = "Latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "Longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "Recorded_At")
    private LocalDateTime recordedAt = LocalDateTime.now();

    public LocationUpdate() {}

    public Integer getLocationUpdateId() {
        return locationUpdateId;
    }

    public void setLocationUpdateId(Integer locationUpdateId) {
        this.locationUpdateId = locationUpdateId;
    }

    public Integer getTripId() {
        return tripId;
    }

    public void setTripId(Integer tripId) {
        this.tripId = tripId;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
