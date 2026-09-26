package com.bustrans.fleettrack.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "bustrip")
@Data
public class BusTrip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TripId")
    private int tripId;

    @Column(name = "TripDate")
    private LocalDate tripDate;

    @Column(name = "StartTime")
    private LocalTime startTime;

    @Column(name = "ETA")
    private LocalTime eta;

    @Column(name = "PickupLocationId")
    private int pickupLocationId;

    @Column(name = "DropLocationId")
    private int dropLocationId;

    @Column(name = "Status")
    private String status;
}