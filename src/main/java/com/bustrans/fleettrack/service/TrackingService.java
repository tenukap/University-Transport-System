package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.BusTrip;
import com.bustrans.fleettrack.entity.LocationUpdate;
import com.bustrans.fleettrack.repository.BusTripRepository;
import com.bustrans.fleettrack.repository.LocationUpdateRepository;
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
