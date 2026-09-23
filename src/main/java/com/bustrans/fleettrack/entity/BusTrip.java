package com.bustrans.fleettrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    @ManyToOne
    @JoinColumn(name = "bus_id")
    private Bus bus;

    @Column(name = "TripDate")
    private LocalDate tripDate;

    @Column(name = "StartTime")
    private LocalTime startTime;

    @Column(name = "ETA")
    private LocalTime eta;

    @ManyToOne
    @JoinColumn(name = "PickupLocationId")
    private Location pickupLocation;

    @ManyToOne
    @JoinColumn(name = "DropLocationId")
    private Location dropLocation;

    @Column(name = "TripStatus")
    private String tripStatus;

    @Column(name = "OperatingCost")
    private BigDecimal operatingCost;
}
