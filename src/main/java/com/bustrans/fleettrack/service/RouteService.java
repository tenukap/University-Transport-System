package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.BusTrip;
import com.bustrans.fleettrack.entity.Location;
import com.bustrans.fleettrack.entity.LocationUpdate;
import com.bustrans.fleettrack.repository.BusTripRepository;
import com.bustrans.fleettrack.repository.LocationUpdateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class RouteService {

    // BusRouteRepository is kept in the project (entity + DB table remain),
    // but the route CRUD feature has been removed from the UI and API.
    @Autowired private BusTripRepository tripRepo;
    @Autowired private LocationUpdateRepository locationUpdateRepo;

    /** Sets status to "Scheduled", saves the trip, and seeds an initial GPS ping. */
    public BusTrip scheduleTrip(BusTrip trip) {
        trip.setTripStatus("Scheduled");
        BusTrip savedTrip = tripRepo.save(trip);

        // Auto-create initial GPS ping from pickup location coordinates (if available).
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
