package com.transport.uni_transport_system.controller;

import com.transport.uni_transport_system.entity.CrashIncident;
import com.transport.uni_transport_system.service.CrashIncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crash-incidents")
public class CrashIncidentController {

    private final CrashIncidentService service;

    @Autowired
    public CrashIncidentController(CrashIncidentService service) {
        this.service = service;
    }

    @GetMapping
    public List<CrashIncident> getAllIncidents() {
        return service.getAllIncidents();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CrashIncident> getIncidentById(@PathVariable Integer id) {
        return service.getIncidentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public CrashIncident createIncident(@RequestBody CrashIncident incident) {
        return service.saveIncident(incident);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CrashIncident> updateIncident(@PathVariable Integer id, @RequestBody CrashIncident incidentDetail) {
        return service.getIncidentById(id)
                .map(existingIncident -> {
                    incidentDetail.setIncidentId(existingIncident.getIncidentId());
                    return ResponseEntity.ok(service.saveIncident(incidentDetail));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncident(@PathVariable Integer id) {
        if (service.getIncidentById(id).isPresent()) {
            service.deleteIncident(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
