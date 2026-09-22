package com.bustrans.fleettrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "bustrip")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TripId")
    private Integer tripId;

    @Column(name = "TripDate")
    private LocalDate tripDate;

    @Column(name = "StartTime")
    private LocalTime startTime;

    @Column(name = "ETA")
    private LocalTime eta;

    @Column(name = "PickupLocationId")
    private Integer pickupLocationId;

    @Column(name = "DropLocationId")
    private Integer dropLocationId;
}
