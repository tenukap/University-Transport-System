package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.BusRoute;
import com.bustrans.fleettrack.entity.BusTrip;
import com.bustrans.fleettrack.entity.Location;
import com.bustrans.fleettrack.entity.LocationUpdate;
import com.bustrans.fleettrack.repository.BusRouteRepository;
import com.bustrans.fleettrack.repository.BusTripRepository;
import com.bustrans.fleettrack.repository.LocationUpdateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RouteService {

    @Autowired private BusRouteRepository routeRepo;
    @Autowired private BusTripRepository tripRepo;
    @Autowired private LocationUpdateRepository locationUpdateRepo;

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
        BusRoute route = routeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Route not found: " + id));
        route.setRouteName(routeDetails.getRouteName());
        route.setStartPoint(routeDetails.getStartPoint());
        route.setEndPoint(routeDetails.getEndPoint());
        return routeRepo.save(route);
    }

    public void deleteRoute(int id) {
        routeRepo.deleteById(id);
    }

    public BusTrip scheduleTrip(BusTrip trip) {
        trip.setTripStatus("Scheduled");
        BusTrip savedTrip = tripRepo.save(trip);

        // Auto-create initial GPS ping from pickup location coordinates (if available)
        Location pickup = trip.getPickupLocation();
        BigDecimal lat = (pickup != null && pickup.getLatitude() != null)
                ? pickup.getLatitude() : new BigDecimal("6.9271");
        BigDecimal lng = (pickup != null && pickup.getLongitude() != null)
                ? pickup.getLongitude() : new BigDecimal("79.8612");

        LocationUpdate initialUpdate = new LocationUpdate();
        initialUpdate.setTripId(savedTrip.getTripId());
        initialUpdate.setLatitude(lat);
        initialUpdate.setLongitude(lng);
        initialUpdate.setRecordedAt(LocalDateTime.now());
        locationUpdateRepo.save(initialUpdate);

        return savedTrip;
    }
}
