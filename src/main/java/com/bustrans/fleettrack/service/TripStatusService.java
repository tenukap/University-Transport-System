package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.TripStatus;
import com.bustrans.fleettrack.repository.TripStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TripStatusService {

    private final TripStatusRepository repository;

    @Autowired
    public TripStatusService(TripStatusRepository repository) {
        this.repository = repository;
    }

    public List<TripStatus> getAllStatuses() {
        return repository.findAll();
    }

    public Optional<TripStatus> getStatusById(Integer id) {
        return repository.findById(id);
    }

    public TripStatus saveStatus(TripStatus status) {
        return repository.save(status);
    }

    public void deleteStatus(Integer id) {
        repository.deleteById(id);
    }
}
