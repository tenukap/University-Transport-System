package com.transport.uni_transport_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Trip_Status")
public class TripStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Status_Id")
    private Integer statusId;

    @Column(name = "Trip_Id", nullable = false)
    private Integer tripId;

    @Column(name = "Status_Type", length = 100)
    private String statusType;

    @Column(name = "Updated_At")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public TripStatus() {}

    // Add Getters and Setters here (Generate them in IntelliJ using Alt+Insert)
}