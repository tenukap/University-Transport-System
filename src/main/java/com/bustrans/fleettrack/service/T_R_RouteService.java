package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.T_R_BusRoute;
import com.bustrans.fleettrack.entity.T_R_BusTrip;
import com.bustrans.fleettrack.entity.T_R_Location;
import com.bustrans.fleettrack.entity.T_R_LocationUpdate;
import com.bustrans.fleettrack.repository.T_R_BusRouteRepository;
import com.bustrans.fleettrack.repository.T_R_BusTripRepository;
import com.bustrans.fleettrack.repository.T_R_LocationRepository;
import com.bustrans.fleettrack.repository.T_R_LocationUpdateRepository;
import com.bustrans.fleettrack.util.T_R_EtaCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class T_R_RouteService {

    @Autowired private T_R_BusRouteRepository routeRepo;
    @Autowired private T_R_BusTripRepository tripRepo;
    @Autowired private T_R_LocationRepository locationRepo;
    @Autowired private T_R_LocationUpdateRepository locationUpdateRepo;

    // ==========================================
    // ROUTE CRUD
    // ==========================================

    public T_R_BusRoute addRoute(T_R_BusRoute route) {
        if (routeRepo.findByRouteName(route.getRouteName()).isPresent()) {
            throw new RuntimeException("Duplicate Route Found!");
        }
        return routeRepo.save(route);
    }

    public List<T_R_BusRoute> getAllRoutes() {
        return routeRepo.findAll();
    }

    public T_R_BusRoute updateRoute(int id, T_R_BusRoute routeDetails) {
        T_R_BusRoute route = routeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Route not found: " + id));

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

    public T_R_BusTrip scheduleTrip(T_R_BusTrip trip) {
        trip.setStatus("Scheduled");

        // Set an initial ETA = start time + estimated travel time
        if (trip.getStartTime() != null) {
            T_R_Location pickup = locationRepo.findById(trip.getPickupLocationId()).orElse(null);
            T_R_Location drop = locationRepo.findById(trip.getDropLocationId()).orElse(null);

            if (pickup != null && drop != null &&
                    pickup.getLatitude() != null && drop.getLatitude() != null) {

                LocalTime initialEta = T_R_EtaCalculator.calculateEta(
                        pickup.getLatitude(), pickup.getLongitude(),
                        drop.getLatitude(), drop.getLongitude()
                );
                trip.setEta(initialEta);
            }
        }

        T_R_BusTrip savedTrip = tripRepo.save(trip);

        // Auto-create initial GPS ping (from previous fix)
        T_R_Location pickupLocation = locationRepo.findById(trip.getPickupLocationId())
                .orElseThrow(() -> new RuntimeException("Pickup location not found"));

        T_R_LocationUpdate initialUpdate = new T_R_LocationUpdate();
        initialUpdate.setTripId(savedTrip.getTripId());
        initialUpdate.setLatitude(pickupLocation.getLatitude() != null ? pickupLocation.getLatitude() : 6.9271);
        initialUpdate.setLongitude(pickupLocation.getLongitude() != null ? pickupLocation.getLongitude() : 79.8612);
        initialUpdate.setRecordedAt(LocalDateTime.now());
        locationUpdateRepo.save(initialUpdate);

        return savedTrip;
    }
}