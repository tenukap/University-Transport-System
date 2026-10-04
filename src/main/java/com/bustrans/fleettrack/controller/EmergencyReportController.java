package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.entity.BusTrip;
import com.bustrans.fleettrack.entity.EmergencyReport;
import com.bustrans.fleettrack.repository.BusTripRepository;
import com.bustrans.fleettrack.service.EmergencyReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/emergency-reports")
public class EmergencyReportController {

    private static final int MAX_TITLE_LEN = 255;
    private static final int MAX_TYPE_LEN  = 100;
    private static final int MAX_DESC_LEN  = 2000;

    private final EmergencyReportService service;
    private final BusTripRepository busTripRepository;

    public EmergencyReportController(EmergencyReportService service,
                                     BusTripRepository busTripRepository) {
        this.service = service;
        this.busTripRepository = busTripRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAllReports(Authentication auth) {
        if (isPrivileged(auth)) {
            return ResponseEntity.ok(service.getAllReports());
        }
        Integer userId = Integer.parseInt(auth.getName());
        return ResponseEntity.ok(service.getReportsByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmergencyReport> getReportById(@PathVariable Integer id) {
        return service.getReportById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createReport(@RequestBody EmergencyReport incoming, Authentication auth) {
        String title = incoming.getReportTitle();
        String type  = incoming.getEmergencyType();
        String desc  = incoming.getDescription();

        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Report title is required"));
        }
        if (title.length() > MAX_TITLE_LEN) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Report title must not exceed " + MAX_TITLE_LEN + " characters"));
        }
        if (type == null || type.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Emergency type is required"));
        }
        if (type.length() > MAX_TYPE_LEN) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Emergency type must not exceed " + MAX_TYPE_LEN + " characters"));
        }
        if (desc == null || desc.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Description is required"));
        }
        if (desc.length() > MAX_DESC_LEN) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Description must not exceed " + MAX_DESC_LEN + " characters"));
        }

        EmergencyReport report = new EmergencyReport();
        report.setReportTitle(title.strip());
        report.setEmergencyType(type.strip());
        report.setDescription(desc.strip());
        report.setResolutionStatus("Pending");
        // Timestamp set server-side so it uses the same JVM clock as TripStatus and LocationUpdate
        report.setTimestamp(LocalDateTime.now());
        // Reporter identity always comes from the token, never from the client payload
        report.setStudentNo(Integer.parseInt(auth.getName()));

        // DRIVER must supply a tripId that belongs to them; STUDENT tripId is optional
        boolean isDriver = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DRIVER"));
        if (isDriver) {
            Integer tripId = incoming.getTripId();
            if (tripId == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "A trip must be selected"));
            }
            BusTrip trip = busTripRepository.findById(tripId).orElse(null);
            if (trip == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Trip not found"));
            }
            Integer driverId = Integer.parseInt(auth.getName());
            if (!driverId.equals(trip.getDriverUserId())) {
                return ResponseEntity.status(403).body(Map.of("message", "This trip is not assigned to you"));
            }
            report.setTripId(tripId);
        }

        return ResponseEntity.ok(service.saveReport(report));
    }

    // Generic PUT/DELETE removed — the transport-officer uses PUT /api/transport/emergency-reports/{id}/status

    private boolean isPrivileged(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_TRANSPORT_OFFICER") ||
                a.getAuthority().equals("ROLE_ADMIN"));
    }
}
