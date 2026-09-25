package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.BusTrip;
import com.bustrans.fleettrack.entity.Location;
import com.bustrans.fleettrack.entity.LocationUpdate;
import com.bustrans.fleettrack.entity.TripCancellation;
import com.bustrans.fleettrack.repository.BusTripRepository;
import com.bustrans.fleettrack.repository.LocationRepository;
import com.bustrans.fleettrack.repository.LocationUpdateRepository;
import com.bustrans.fleettrack.repository.TripCancellationRepository;
import com.bustrans.fleettrack.util.EtaCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class TripService {
    @Autowired private BusTripRepository tripRepo;
    @Autowired private TripCancellationRepository cancelRepo;
    @Autowired private LocationUpdateRepository locationRepo;
    @Autowired private LocationRepository locationLookupRepo;

    // GET all trips (NEW - needed by frontend)
    public List<BusTrip> getAllTrips() {
        return tripRepo.findAll();
    }

    // GET one trip by ID (NEW)
    public BusTrip getTripById(int id) {
        return tripRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));
    }

    // CANCEL Trip (From your Cancel Bus Trip Sequence Diagram)
    public BusTrip cancelTrip(int tripId, String reason) {
        BusTrip trip = tripRepo.findById(tripId).orElseThrow();
        trip.setStatus("Cancelled");
        tripRepo.save(trip);

        // Save cancellation record
        TripCancellation cancellation = new TripCancellation();
        cancellation.setTripId(tripId);
        cancellation.setReason(reason);
        cancellation.setCancelledAt(LocalDateTime.now());
        cancelRepo.save(cancellation);

        return trip;
    }

    // UPDATE Location & ETA (From Driver Progress Sequence Diagram)
    public void updateLocation(int tripId, double lat, double lng) {
        // 1. Save the location update
        LocationUpdate update = new LocationUpdate();
        update.setTripId(tripId);
        update.setLatitude(lat);
        update.setLongitude(lng);
        update.setRecordedAt(LocalDateTime.now());
        locationRepo.save(update);

        // 2. Get the trip + its drop location
        BusTrip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        Location dropLocation = locationLookupRepo.findById(trip.getDropLocationId())
                .orElseThrow(() -> new RuntimeException("Drop location not found"));

        // 3. Calculate ETA based on distance to destination
        if (dropLocation.getLatitude() != null && dropLocation.getLongitude() != null) {
            LocalTime newEta = EtaCalculator.calculateEta(
                    lat, lng,
                    dropLocation.getLatitude(),
                    dropLocation.getLongitude()
            );
            trip.setEta(newEta);

            // 4. Auto-complete if within 200 meters of destination
            double distKm = EtaCalculator.calculateDistance(
                    lat, lng,
                    dropLocation.getLatitude(),
                    dropLocation.getLongitude()
            );
            if (distKm < 0.2) {
                trip.setStatus("Completed");
            }

            tripRepo.save(trip);
        }
    }

    // COMPLETE Trip (From Driver Progress Sequence Diagram)
    public void completeTrip(int tripId) {
        BusTrip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));
        trip.setStatus("Completed");
        tripRepo.save(trip);
    }
}