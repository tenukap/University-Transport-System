package com.transport.uni_transport_system.entity;

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
    private int tripId;

    private LocalDate tripDate;
    private LocalTime startTime;
    private LocalTime eta;

    @Column(name = "pickup_location_id")
    private int pickupLocationId;

    @Column(name = "drop_location_id")
    private int dropLocationId;

    private String status; // "Scheduled", "Cancelled", "Completed"
}