package com.bustrans.fleettrack.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "destination")
@Data
public class Destination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DestinationId")
    private int destinationId;

    @Column(name = "DestinationName")
    private String destinationName;

    @Column(name = "Location")
    private String location;

    @Column(name = "RouteId")
    private int routeId;
}