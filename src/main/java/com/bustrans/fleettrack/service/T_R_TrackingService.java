package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.T_R_BusTrip;
import com.bustrans.fleettrack.entity.T_R_LocationUpdate;
import com.bustrans.fleettrack.repository.T_R_BusTripRepository;
import com.bustrans.fleettrack.repository.T_R_LocationUpdateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class T_R_TrackingService {
    @Autowired
    private T_R_LocationUpdateRepository locationRepo;
    @Autowired
    private T_R_BusTripRepository tripRepo;

    public void updateLocation(int tripId, double lat, double lng) {
        T_R_LocationUpdate update = new T_R_LocationUpdate();
        update.setTripId(tripId);
        update.setLatitude(lat);
        update.setLongitude(lng);
        update.setRecordedAt(LocalDateTime.now());
        locationRepo.save(update);
    }

    public void markTripComplete(int tripId) {
        T_R_BusTrip trip = tripRepo.findById(tripId).orElseThrow();
        trip.setStatus("Completed");
        tripRepo.save(trip);
    }
}
