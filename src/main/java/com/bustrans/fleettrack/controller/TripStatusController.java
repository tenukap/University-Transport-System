package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.entity.BusTrip;
import com.bustrans.fleettrack.entity.TripStatus;
import com.bustrans.fleettrack.repository.BusTripRepository;
import com.bustrans.fleettrack.service.TripStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/trip-statuses")
public class TripStatusController {

    private static final Set<String> VALID_STATUS_TYPES = Set.of(
            "Scheduled", "Departed", "In Progress", "Delayed", "At Stop", "Completed");

    private final TripStatusService service;
    private final BusTripRepository busTripRepository;

    public TripStatusController(TripStatusService service, BusTripRepository busTripRepository) {
        this.service = service;
        this.busTripRepository = busTripRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAllStatuses(Authentication auth) {
        if (isPrivileged(auth)) {
            return ResponseEntity.ok(service.getAllStatuses());
        }
        // DRIVER: only rows for their assigned trips
        Integer driverId = Integer.parseInt(auth.getName());
        List<Integer> tripIds = busTripRepository.findTripIdsByDriver(driverId);
        return ResponseEntity.ok(service.getStatusesForTrips(tripIds));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripStatus> getStatusById(@PathVariable Integer id) {
        return service.getStatusById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createStatus(@RequestBody TripStatus incoming, Authentication auth) {
        Integer driverId = Integer.parseInt(auth.getName());
        Integer tripId = incoming.getTripId();
        String statusType = incoming.getStatusType();

        if (tripId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "tripId is required"));
        }
        if (statusType == null || !VALID_STATUS_TYPES.contains(statusType)) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "statusType must be one of: Scheduled, Departed, In Progress, Delayed, At Stop, Completed"));
        }

        BusTrip trip = busTripRepository.findById(tripId).orElse(null);
        if (trip == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Trip not found"));
        }
        if (!driverId.equals(trip.getDriverUserId())) {
            return ResponseEntity.status(403).body(Map.of("message", "This trip is not assigned to you"));
        }
        if ("Cancelled".equalsIgnoreCase(trip.getTripStatus())) {
            return ResponseEntity.badRequest().body(Map.of("message", "This trip has been cancelled"));
        }

        // Completing a trip marks it so it leaves the booking list
        if ("Completed".equals(statusType)) {
            trip.setTripStatus("Completed");
            busTripRepository.save(trip);
        }

        TripStatus status = new TripStatus();
        status.setTripId(tripId);
        status.setStatusType(statusType);
        return ResponseEntity.ok(service.saveStatus(status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TripStatus> updateStatus(@PathVariable Integer id,
                                                   @RequestBody TripStatus detail) {
        return service.getStatusById(id)
                .map(existing -> {
                    detail.setStatusId(existing.getStatusId());
                    return ResponseEntity.ok(service.saveStatus(detail));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStatus(@PathVariable Integer id) {
        if (service.getStatusById(id).isPresent()) {
            service.deleteStatus(id);
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
