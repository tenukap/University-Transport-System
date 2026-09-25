package com.transport.uni_transport_system.service;

import com.transport.uni_transport_system.entity.BusRoute;
import com.transport.uni_transport_system.entity.BusTrip;
import com.transport.uni_transport_system.entity.Location;
import com.transport.uni_transport_system.entity.LocationUpdate;
import com.transport.uni_transport_system.repository.BusRouteRepository;
import com.transport.uni_transport_system.repository.BusTripRepository;
import com.transport.uni_transport_system.repository.LocationRepository;
import com.transport.uni_transport_system.repository.LocationUpdateRepository;
import com.transport.uni_transport_system.util.EtaCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class RouteService {

    @Autowired private BusRouteRepository routeRepo;
    @Autowired private BusTripRepository tripRepo;
    @Autowired private LocationRepository locationRepo;
    @Autowired private LocationUpdateRepository locationUpdateRepo;

    // ==========================================
    // ROUTE CRUD
    // ==========================================

    public BusRoute addRoute(BusRoute route) {
        if (routeRepo.findByRouteName(route.getRouteName()).isPresent()) {
            throw new RuntimeException("Duplicate Route Found!");
        }
        return routeRepo.save(route);
    }

    public List<BusRoute> getAllRoutes() {
        return routeRepo.findAll();
    }

    public BusRoute updateRoute(int id, BusRoute routeDetails) {
        BusRoute route = routeRepo.findById(id).orElseThrow();
        route.setRouteName(routeDetails.getRouteName());
        route.setStartPoint(routeDetails.getStartPoint());
        route.setEndPoint(routeDetails.getEndPoint());
        return routeRepo.save(route);
    }

    public void deleteRoute(int id) {
        routeRepo.deleteById(id);
    }

    // ==========================================
    // SCHEDULE TRIP + AUTO-CREATE INITIAL GPS PING
    // ==========================================

    public BusTrip scheduleTrip(BusTrip trip) {
        trip.setStatus("Scheduled");

        // Set an initial ETA = start time + estimated travel time
        if (trip.getStartTime() != null) {
            Location pickup = locationRepo.findById(trip.getPickupLocationId()).orElse(null);
            Location drop = locationRepo.findById(trip.getDropLocationId()).orElse(null);

            if (pickup != null && drop != null &&
                    pickup.getLatitude() != null && drop.getLatitude() != null) {

                LocalTime initialEta = EtaCalculator.calculateEta(
                        pickup.getLatitude(), pickup.getLongitude(),
                        drop.getLatitude(), drop.getLongitude()
                );
                trip.setEta(initialEta);
            }
        }

        BusTrip savedTrip = tripRepo.save(trip);

        // Auto-create initial GPS ping (from previous fix)
        Location pickupLocation = locationRepo.findById(trip.getPickupLocationId())
                .orElseThrow(() -> new RuntimeException("Pickup location not found"));

        LocationUpdate initialUpdate = new LocationUpdate();
        initialUpdate.setTripId(savedTrip.getTripId());
        initialUpdate.setLatitude(pickupLocation.getLatitude() != null ? pickupLocation.getLatitude() : 6.9271);
        initialUpdate.setLongitude(pickupLocation.getLongitude() != null ? pickupLocation.getLongitude() : 79.8612);
        initialUpdate.setRecordedAt(LocalDateTime.now());
        locationUpdateRepo.save(initialUpdate);

        return savedTrip;
    }
}