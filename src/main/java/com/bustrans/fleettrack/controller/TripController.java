package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.dto.TripResponseDTO;
import com.bustrans.fleettrack.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @GetMapping
    public List<TripResponseDTO> getAllTrips() {
        return tripService.getAllTrips();
    }

    @GetMapping("/{id}")
    public TripResponseDTO getTripById(@PathVariable Integer id) {
        return tripService.getTripById(id);
    }
}
