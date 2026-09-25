package com.transport.uni_transport_system.service;

import com.transport.uni_transport_system.entity.Location;
import com.transport.uni_transport_system.repository.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationService {

    @Autowired
    private LocationRepository locationRepo;

    public List<Location> getAllLocations() {
        return locationRepo.findAll();
    }

    public Location addLocation(Location location) {
        return locationRepo.save(location);
    }

    public void deleteLocation(int id) {
        locationRepo.deleteById(id);
    }
}