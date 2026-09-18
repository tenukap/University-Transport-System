package com.transport.uni_transport_system.service;

import com.transport.uni_transport_system.dto.LocationResponseDTO;
import com.transport.uni_transport_system.entity.Location;
import com.transport.uni_transport_system.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
                .orElseThrow(() -> new RuntimeException("Location not found"));
        return mapToDTO(location);
    }

    private LocationResponseDTO mapToDTO(Location location) {
        return LocationResponseDTO.builder()
                .locationId(location.getLocationId())
                .locationName(location.getLocationName())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
    }
}
