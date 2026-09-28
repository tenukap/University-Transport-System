package com.bustrans.fleettrack.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "busroute")
@Data
public class T_R_BusRoute {
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
