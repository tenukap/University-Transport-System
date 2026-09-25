package com.transport.uni_transport_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "busroute")
@Data
public class BusRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RouteId") // Tell Hibernate the exact column name
    private int routeId;

    @Column(name = "RouteName")
    private String routeName;

    @Column(name = "StartPoint")
    private String startPoint;

    @Column(name = "EndPoint")
    private String endPoint;
}
