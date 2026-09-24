package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.dto.LocationResponseDTO;
import com.bustrans.fleettrack.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    public List<LocationResponseDTO> getAllLocations() {
        return locationService.getAllLocations();
    }

    @GetMapping("/{id}")
    public LocationResponseDTO getLocationById(@PathVariable Integer id) {
        return locationService.getLocationById(id);
    }
}
