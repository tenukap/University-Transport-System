package com.transport.uni_transport_system.entity;

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

    // Add Getters and Setters here (Generate them in IntelliJ using Alt+Insert)
}