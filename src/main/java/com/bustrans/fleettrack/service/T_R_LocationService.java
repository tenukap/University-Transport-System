package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.T_R_Location;
import com.bustrans.fleettrack.repository.T_R_LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class T_R_LocationService {

    @Autowired
    private T_R_LocationRepository locationRepo;

    public List<T_R_Location> getAllLocations() {
        return locationRepo.findAll();
    }

    public T_R_Location addLocation(T_R_Location location) {
        return locationRepo.save(location);
    }

    public T_R_Location updateLocation(int id, T_R_Location updated) {
        T_R_Location existing = locationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found: " + id));

        existing.setLocationName(updated.getLocationName());
        existing.setLatitude(updated.getLatitude());
        existing.setLongitude(updated.getLongitude());

        return locationRepo.save(existing);
    }

    public void deleteLocation(int id) {
        locationRepo.deleteById(id);
    }
}