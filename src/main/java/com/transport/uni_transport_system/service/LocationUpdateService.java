package com.transport.uni_transport_system.service;

import com.transport.uni_transport_system.entity.LocationUpdate;
import com.transport.uni_transport_system.repository.LocationUpdateRepository;
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
