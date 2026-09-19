package com.transport.uni_transport_system.controller;

import com.transport.uni_transport_system.entity.TripStatus;
import com.transport.uni_transport_system.service.TripStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trip-statuses")
public class TripStatusController {

    private final TripStatusService service;

    @Autowired
    public TripStatusController(TripStatusService service) {
        this.service = service;
    }

    @GetMapping
    public List<TripStatus> getAllStatuses() {
        return service.getAllStatuses();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripStatus> getStatusById(@PathVariable Integer id) {
        return service.getStatusById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public TripStatus createStatus(@RequestBody TripStatus status) {
        return service.saveStatus(status);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TripStatus> updateStatus(@PathVariable Integer id, @RequestBody TripStatus statusDetail) {
        return service.getStatusById(id)
                .map(existingStatus -> {
                    statusDetail.setStatusId(existingStatus.getStatusId());
                    return ResponseEntity.ok(service.saveStatus(statusDetail));
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
}
