package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.CrashIncident;
import com.bustrans.fleettrack.repository.CrashIncidentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CrashIncidentService {

    private final CrashIncidentRepository repository;

    @Autowired
    public CrashIncidentService(CrashIncidentRepository repository) {
        this.repository = repository;
    }

    public List<CrashIncident> getAllIncidents() {
        return repository.findAll();
    }

    public Optional<CrashIncident> getIncidentById(Integer id) {
        return repository.findById(id);
    }

    public CrashIncident saveIncident(CrashIncident incident) {
        return repository.save(incident);
    }

    public void deleteIncident(Integer id) {
        repository.deleteById(id);
    }
}
