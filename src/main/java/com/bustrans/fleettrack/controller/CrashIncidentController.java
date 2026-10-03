package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.entity.BusTrip;
import com.bustrans.fleettrack.entity.CrashIncident;
import com.bustrans.fleettrack.repository.BusTripRepository;
import com.bustrans.fleettrack.service.CrashIncidentService;
import org.springframework.http.ResponseEntity;
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
    private final BusTripRepository busTripRepository;

    public CrashIncidentController(CrashIncidentService service,
                                   BusTripRepository busTripRepository) {
        this.service = service;
        this.busTripRepository = busTripRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAllIncidents(Authentication auth) {
        if (isPrivileged(auth)) {
            return ResponseEntity.ok(service.getAllIncidents());
        }
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

        Integer driverId = Integer.parseInt(auth.getName());

        Integer tripId = incoming.getTripId();
        if (tripId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "A trip must be selected"));
        }
        BusTrip trip = busTripRepository.findById(tripId).orElse(null);
        if (trip == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Trip not found"));
        }
        if (!driverId.equals(trip.getDriverUserId())) {
            return ResponseEntity.status(403).body(Map.of("message", "This trip is not assigned to you"));
        }

        CrashIncident incident = new CrashIncident();
        incident.setLocationCoordinates(location.strip());
        incident.setSeverityLevel(severity.strip());
        incident.setDescription(desc != null ? desc.strip() : null);
        incident.setStatus("Pending");
        // driverUserId, busNo, tripId, and timestamp always set server-side
        incident.setDriverUserId(driverId);
        incident.setTripId(tripId);
        // Copy the bus from the trip so the crash is linked to the correct bus
        if (trip.getBus() != null) {
            incident.setBusNo(trip.getBus().getBusId());
        }
        incident.setTimestamp(LocalDateTime.now());

        return ResponseEntity.ok(service.saveIncident(incident));
    }

    // Generic PUT/DELETE removed — the transport-officer uses PUT /api/transport/crash-incidents/{id}/status

    private boolean isPrivileged(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_TRANSPORT_OFFICER") ||
                a.getAuthority().equals("ROLE_ADMIN"));
    }
}
