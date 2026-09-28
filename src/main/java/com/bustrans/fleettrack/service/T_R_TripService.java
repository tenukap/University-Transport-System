package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.T_R_BusTrip;
import com.bustrans.fleettrack.entity.T_R_Location;
import com.bustrans.fleettrack.entity.T_R_LocationUpdate;
import com.bustrans.fleettrack.entity.T_R_TripCancellation;
import com.bustrans.fleettrack.repository.T_R_BusTripRepository;
import com.bustrans.fleettrack.repository.T_R_LocationRepository;
import com.bustrans.fleettrack.repository.T_R_LocationUpdateRepository;
import com.bustrans.fleettrack.repository.T_R_TripCancellationRepository;
import com.bustrans.fleettrack.util.T_R_EtaCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class T_R_TripService {
    @Autowired private T_R_BusTripRepository tripRepo;
    @Autowired private T_R_TripCancellationRepository cancelRepo;
    @Autowired private T_R_LocationUpdateRepository locationRepo;
    @Autowired private T_R_LocationRepository locationLookupRepo;

    // GET all trips (NEW - needed by frontend)
    public List<T_R_BusTrip> getAllTrips() {
        return tripRepo.findAll();
    }

    // GET one trip by ID (NEW)
    public T_R_BusTrip getTripById(int id) {
        return tripRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));
    }

    // CANCEL Trip (From your Cancel Bus Trip Sequence Diagram)
    public T_R_BusTrip cancelTrip(int tripId, String reason) {
        T_R_BusTrip trip = tripRepo.findById(tripId).orElseThrow();
        trip.setStatus("Cancelled");
        tripRepo.save(trip);

        // Save cancellation record
        T_R_TripCancellation cancellation = new T_R_TripCancellation();
        cancellation.setTripId(tripId);
        cancellation.setReason(reason);
        cancellation.setCancelledAt(LocalDateTime.now());
        cancelRepo.save(cancellation);

        return trip;
    }

    // UPDATE a trip
    public T_R_BusTrip updateTrip(int id, T_R_BusTrip updatedTrip) {
        T_R_BusTrip existing = tripRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + id));

        existing.setTripDate(updatedTrip.getTripDate());
        existing.setStartTime(updatedTrip.getStartTime());
        existing.setEta(updatedTrip.getEta());
        existing.setPickupLocationId(updatedTrip.getPickupLocationId());
        existing.setDropLocationId(updatedTrip.getDropLocationId());
        existing.setStatus(updatedTrip.getStatus());

        return tripRepo.save(existing);
    }


    // UPDATE Location & ETA (From Driver Progress Sequence Diagram)
    public void updateLocation(int tripId, double lat, double lng) {
        // 1. Save the location update
        T_R_LocationUpdate update = new T_R_LocationUpdate();
        update.setTripId(tripId);
        update.setLatitude(lat);
        update.setLongitude(lng);
        update.setRecordedAt(LocalDateTime.now());
        locationRepo.save(update);

        // 2. Get the trip + its drop location
        T_R_BusTrip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        T_R_Location dropLocation = locationLookupRepo.findById(trip.getDropLocationId())
                .orElseThrow(() -> new RuntimeException("Drop location not found"));

        // 3. Calculate ETA based on distance to destination
        if (dropLocation.getLatitude() != null && dropLocation.getLongitude() != null) {
            LocalTime newEta = T_R_EtaCalculator.calculateEta(
                    lat, lng,
                    dropLocation.getLatitude(),
                    dropLocation.getLongitude()
            );
            trip.setEta(newEta);

            // 4. Auto-complete if within 200 meters of destination
            double distKm = T_R_EtaCalculator.calculateDistance(
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
        T_R_BusTrip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));
        trip.setStatus("Completed");
        tripRepo.save(trip);
    }
}