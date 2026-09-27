package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.dto.LocationResponseDTO;
import com.bustrans.fleettrack.entity.Location;
import com.bustrans.fleettrack.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;

    public List<LocationResponseDTO> getAllLocations() {
        return locationRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public LocationResponseDTO getLocationById(Integer id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + id));
        return mapToDTO(location);
    }

    private LocationResponseDTO mapToDTO(Location location) {
        return LocationResponseDTO.builder()
                .locationId(location.getLocationId())
                .locationName(location.getLocationName())
                .latitude(toDouble(location.getLatitude()))
                .longitude(toDouble(location.getLongitude()))
                .build();
    }

    private Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }
}
