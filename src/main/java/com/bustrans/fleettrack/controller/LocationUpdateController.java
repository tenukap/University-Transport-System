package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.entity.BusTrip;
import com.bustrans.fleettrack.entity.LocationUpdate;
import com.bustrans.fleettrack.repository.BusTripRepository;
import com.bustrans.fleettrack.service.LocationUpdateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/location-updates")
public class LocationUpdateController {

    private final LocationUpdateService service;
    private final BusTripRepository busTripRepository;

    public LocationUpdateController(LocationUpdateService service, BusTripRepository busTripRepository) {
        this.service = service;
        this.busTripRepository = busTripRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAllUpdates(Authentication auth) {
        if (isPrivileged(auth)) {
            return ResponseEntity.ok(service.getAllUpdates());
        }
        // DRIVER: only their assigned trips' updates
        Integer driverId = Integer.parseInt(auth.getName());
        List<Integer> tripIds = busTripRepository.findTripIdsByDriver(driverId);
        return ResponseEntity.ok(service.getUpdatesForTrips(tripIds));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationUpdate> getUpdateById(@PathVariable Integer id) {
        return service.getUpdateById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUpdate(@RequestBody LocationUpdate incoming, Authentication auth) {
        Integer driverId = Integer.parseInt(auth.getName());
        Integer tripId = incoming.getTripId();
        BigDecimal lat = incoming.getLatitude();
        BigDecimal lng = incoming.getLongitude();

        if (tripId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "tripId is required"));
        }
        if (lat == null || lng == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "latitude and longitude are required"));
        }
        if (lat.compareTo(BigDecimal.valueOf(-90)) < 0 || lat.compareTo(BigDecimal.valueOf(90)) > 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Latitude must be between -90 and 90"));
        }
        if (lng.compareTo(BigDecimal.valueOf(-180)) < 0 || lng.compareTo(BigDecimal.valueOf(180)) > 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Longitude must be between -180 and 180"));
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

        LocationUpdate update = new LocationUpdate();
        update.setTripId(tripId);
        update.setLatitude(lat);
        update.setLongitude(lng);
        return ResponseEntity.ok(service.saveUpdate(update));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocationUpdate> updateUpdate(@PathVariable Integer id,
                                                       @RequestBody LocationUpdate detail) {
        return service.getUpdateById(id)
                .map(existing -> {
                    detail.setLocationUpdateId(existing.getLocationUpdateId());
                    return ResponseEntity.ok(service.saveUpdate(detail));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUpdate(@PathVariable Integer id) {
        if (service.getUpdateById(id).isPresent()) {
            service.deleteUpdate(id);
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
