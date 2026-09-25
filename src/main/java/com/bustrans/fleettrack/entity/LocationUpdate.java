package com.bustrans.fleettrack.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "locationupdate")
@Data
public class LocationUpdate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LocationUpdateId")
    private int locationUpdateId;

    @Column(name = "TripId")
    private int tripId;

    @Column(name = "Latitude")
    private double latitude;

    @Column(name = "Longitude")
    private double longitude;

    @Column(name = "RecordedAt")
    private LocalDateTime recordedAt;
}