package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.entity.CrashIncident;
import com.bustrans.fleettrack.service.CrashIncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/crash-incidents")
public class CrashIncidentController {

    private static final int MAX_LOCATION_LEN = 255;
    private static final int MAX_SEVERITY_LEN = 50;
    private static final int MAX_DESC_LEN     = 2000;

    private final CrashIncidentService service;

    public CrashIncidentController(CrashIncidentService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> getAllIncidents(Authentication auth) {
        if (isPrivileged(auth)) {
            return ResponseEntity.ok(service.getAllIncidents());
        }
        // DRIVER sees only their own incidents
        Integer driverId = Integer.parseInt(auth.getName());
        return ResponseEntity.ok(service.getIncidentsByDriver(driverId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CrashIncident> getIncidentById(@PathVariable Integer id) {
        return service.getIncidentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createIncident(@RequestBody CrashIncident incoming, Authentication auth) {
        String location = incoming.getLocationCoordinates();
        String severity = incoming.getSeverityLevel();
        String desc     = incoming.getDescription();

        if (location == null || location.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Location / coordinates are required"));
        }
        if (location.length() > MAX_LOCATION_LEN) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Location must not exceed " + MAX_LOCATION_LEN + " characters"));
        }
        if (severity == null || severity.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Severity level is required"));
        }
        if (severity.length() > MAX_SEVERITY_LEN) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Severity level must not exceed " + MAX_SEVERITY_LEN + " characters"));
        }
        if (desc != null && desc.length() > MAX_DESC_LEN) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Description must not exceed " + MAX_DESC_LEN + " characters"));
        }

        CrashIncident incident = new CrashIncident();
        incident.setLocationCoordinates(location.strip());
        incident.setSeverityLevel(severity.strip());
        incident.setDescription(desc != null ? desc.strip() : null);
        incident.setStatus("Reported");
        // driverUserId and timestamp always set server-side
        incident.setDriverUserId(Integer.parseInt(auth.getName()));
        incident.setTimestamp(LocalDateTime.now());

        return ResponseEntity.ok(service.saveIncident(incident));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TRANSPORT_OFFICER')")
    public ResponseEntity<CrashIncident> updateIncident(@PathVariable Integer id,
                                                        @RequestBody CrashIncident detail) {
        return service.getIncidentById(id)
                .map(existing -> {
                    detail.setIncidentId(existing.getIncidentId());
                    return ResponseEntity.ok(service.saveIncident(detail));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TRANSPORT_OFFICER')")
    public ResponseEntity<Void> deleteIncident(@PathVariable Integer id) {
        if (service.getIncidentById(id).isPresent()) {
            service.deleteIncident(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private boolean isPrivileged(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_TRANSPORT_OFFICER") ||
                a.getAuthority().equals("ROLE_ADMIN"));
    }
}
