package com.transport.uni_transport_system.service;

import com.transport.uni_transport_system.entity.EmergencyReport;
import com.transport.uni_transport_system.repository.EmergencyReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmergencyReportService {

    private final EmergencyReportRepository repository;

    @Autowired
    public EmergencyReportService(EmergencyReportRepository repository) {
        this.repository = repository;
    }

    public List<EmergencyReport> getAllReports() {
        return repository.findAll();
    }

    public Optional<EmergencyReport> getReportById(Integer id) {
        return repository.findById(id);
    }

    public EmergencyReport saveReport(EmergencyReport report) {
        return repository.save(report);
    }

    public void deleteReport(Integer id) {
        repository.deleteById(id);
    }
}
