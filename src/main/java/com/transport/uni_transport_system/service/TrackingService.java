package com.transport.uni_transport_system.service;

import com.transport.uni_transport_system.entity.BusTrip;
import com.transport.uni_transport_system.entity.LocationUpdate;
import com.transport.uni_transport_system.repository.BusTripRepository;
import com.transport.uni_transport_system.repository.LocationUpdateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TrackingService {
    @Autowired
    private LocationUpdateRepository locationRepo;
    @Autowired
    private BusTripRepository tripRepo;

    public void updateLocation(int tripId, double lat, double lng) {
        LocationUpdate update = new LocationUpdate();
        update.setTripId(tripId);
        update.setLatitude(lat);
        update.setLongitude(lng);
        update.setRecordedAt(LocalDateTime.now());
        locationRepo.save(update);
    }

    public void markTripComplete(int tripId) {
        BusTrip trip = tripRepo.findById(tripId).orElseThrow();
        trip.setStatus("Completed");
        tripRepo.save(trip);
    }
}
