package com.transport.uni_transport_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "locationupdate")
@Data
public class LocationUpdate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int locationUpdateId;

    @Column(name = "trip_id")
    private int tripId;

    private double latitude;
    private double longitude;
    private LocalDateTime recordedAt;
}
