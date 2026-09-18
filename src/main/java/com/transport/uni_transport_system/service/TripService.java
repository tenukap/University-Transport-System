package com.transport.uni_transport_system.service;

import com.transport.uni_transport_system.entity.BusTrip;
import com.transport.uni_transport_system.entity.LocationUpdate;
import com.transport.uni_transport_system.entity.TripCancellation;
import com.transport.uni_transport_system.repository.BusTripRepository;
import com.transport.uni_transport_system.repository.LocationUpdateRepository;
import com.transport.uni_transport_system.repository.TripCancellationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class TripService {
    @Autowired private BusTripRepository tripRepo;
    @Autowired private TripCancellationRepository cancelRepo;
    @Autowired private LocationUpdateRepository locationRepo;

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
        LocationUpdate update = new LocationUpdate();
        update.setTripId(tripId);
        update.setLatitude(lat);
        update.setLongitude(lng);
        update.setRecordedAt(LocalDateTime.now());
        locationRepo.save(update);
    }

    // COMPLETE Trip (From Driver Progress Sequence Diagram)
    public void completeTrip(int tripId) {
        BusTrip trip = tripRepo.findById(tripId).orElseThrow();
        trip.setStatus("Completed");
        tripRepo.save(trip);
    }
}