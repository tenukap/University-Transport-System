package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.LocationUpdate;
import com.bustrans.fleettrack.repository.LocationUpdateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LocationUpdateService {

    private final LocationUpdateRepository repository;

    @Autowired
    public LocationUpdateService(LocationUpdateRepository repository) {
        this.repository = repository;
    }

    public List<LocationUpdate> getAllUpdates() {
        return repository.findAll();
    }

    public Optional<LocationUpdate> getUpdateById(Integer id) {
        return repository.findById(id);
    }

    public LocationUpdate saveUpdate(LocationUpdate update) {
        return repository.save(update);
    }

    public void deleteUpdate(Integer id) {
        repository.deleteById(id);
    }
}
