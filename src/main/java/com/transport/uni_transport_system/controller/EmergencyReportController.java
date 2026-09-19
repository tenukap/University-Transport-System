package com.transport.uni_transport_system.controller;

import com.transport.uni_transport_system.entity.EmergencyReport;
import com.transport.uni_transport_system.service.EmergencyReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emergency-reports")
public class EmergencyReportController {

    private final EmergencyReportService service;

    @Autowired
    public EmergencyReportController(EmergencyReportService service) {
        this.service = service;
    }

    @GetMapping
    public List<EmergencyReport> getAllReports() {
        return service.getAllReports();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmergencyReport> getReportById(@PathVariable Integer id) {
        return service.getReportById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public EmergencyReport createReport(@RequestBody EmergencyReport report) {
        return service.saveReport(report);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmergencyReport> updateReport(@PathVariable Integer id, @RequestBody EmergencyReport reportDetail) {
        return service.getReportById(id)
                .map(existingReport -> {
                    reportDetail.setReportId(existingReport.getReportId());
                    return ResponseEntity.ok(service.saveReport(reportDetail));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable Integer id) {
        if (service.getReportById(id).isPresent()) {
            service.deleteReport(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
