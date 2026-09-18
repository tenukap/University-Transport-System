package com.transport.uni_transport_system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "destination")
@Data
public class Destination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int destinationId;

    private String destinationName;
    private String location;

    @Column(name = "route_id")
    private int routeId; // Links to BusRoute
}
