package com.transport.uni_transport_system.service;

import com.transport.uni_transport_system.entity.BusRoute;
import com.transport.uni_transport_system.entity.BusTrip;
import com.transport.uni_transport_system.repository.BusRouteRepository;
import com.transport.uni_transport_system.repository.BusTripRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RouteService {
    @Autowired private BusRouteRepository routeRepo;
    @Autowired private BusTripRepository tripRepo;

    // CREATE Route (CRUD)
    public BusRoute addRoute(BusRoute route) {
        if (routeRepo.findByRouteName(route.getRouteName()).isPresent()) {
            throw new RuntimeException("Duplicate Route Found!");
        }
        return routeRepo.save(route);
    }

    // READ Routes (CRUD)
    public List<BusRoute> getAllRoutes() {
        return routeRepo.findAll();
    }

    // UPDATE Route (CRUD)
    public BusRoute updateRoute(int id, BusRoute routeDetails) {
        BusRoute route = routeRepo.findById(id).orElseThrow();
        route.setRouteName(routeDetails.getRouteName());
        route.setStartPoint(routeDetails.getStartPoint());
        route.setEndPoint(routeDetails.getEndPoint());
        return routeRepo.save(route);
    }

    // DELETE Route (CRUD)
    public void deleteRoute(int id) {
        routeRepo.deleteById(id);
    }

    // CREATE Trip (From your Add New Bus Trip Sequence Diagram)
    public BusTrip scheduleTrip(BusTrip trip) {
        trip.setStatus("Scheduled");
        return tripRepo.save(trip);
    }
}