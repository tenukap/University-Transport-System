package com.transport.uni_transport_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "busroute")
@Data @NoArgsConstructor @AllArgsConstructor
public class BusRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int routeId;

    @Column(nullable = false)
    private String routeName;

    private String startPoint;
    private String endPoint;
}
