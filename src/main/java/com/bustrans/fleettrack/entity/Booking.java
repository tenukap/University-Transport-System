package com.bustrans.fleettrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "trip_id")
    private BusTrip busTrip;

    @ManyToOne
    @JoinColumn(name = "pickup_loc_id")
    private Location pickupLocation;

    @ManyToOne
    @JoinColumn(name = "dropoff_loc_id")
    private Location dropoffLocation;

    @Column(name = "seat_number")
    private Integer seatNumber;

    @Column(name = "fare_amount")
    private BigDecimal fareAmount;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
