package com.transport.uni_transport_system.controller;

import com.transport.uni_transport_system.entity.LocationUpdate;
import com.transport.uni_transport_system.service.LocationUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location-updates")
public class LocationUpdateController {

    private final LocationUpdateService service;

    @Autowired
    public LocationUpdateController(LocationUpdateService service) {
        this.service = service;
    }

    @GetMapping
    public List<LocationUpdate> getAllUpdates() {
        return service.getAllUpdates();
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationUpdate> getUpdateById(@PathVariable Integer id) {
        return service.getUpdateById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public LocationUpdate createUpdate(@RequestBody LocationUpdate update) {
        return service.saveUpdate(update);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocationUpdate> updateUpdate(@PathVariable Integer id, @RequestBody LocationUpdate updateDetail) {
        return service.getUpdateById(id)
                .map(existingUpdate -> {
                    updateDetail.setLocationUpdateId(existingUpdate.getLocationUpdateId());
                    return ResponseEntity.ok(service.saveUpdate(updateDetail));
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
}
