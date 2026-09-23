package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.dto.TripResponseDTO;
import com.bustrans.fleettrack.entity.BusTrip;
import com.bustrans.fleettrack.entity.Location;
import com.bustrans.fleettrack.repository.BusTripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {

    private final BusTripRepository busTripRepository;

    public List<TripResponseDTO> getAllTrips() {
        return busTripRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public TripResponseDTO getTripById(Integer id) {
        BusTrip trip = busTripRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trip " + id + " was not found"));
        return mapToDTO(trip);
    }

    private TripResponseDTO mapToDTO(BusTrip trip) {
        Location pickup = trip.getPickupLocation();
        Location drop = trip.getDropLocation();
        return TripResponseDTO.builder()
                .tripId(trip.getTripId())
                .tripDate(trip.getTripDate() != null ? trip.getTripDate().toString() : null)
                .startTime(trip.getStartTime() != null ? trip.getStartTime().toString() : null)
                .eta(trip.getEta() != null ? trip.getEta().toString() : null)
                .tripStatus(trip.getTripStatus())
                .operatingCost(trip.getOperatingCost())
                .pickupLocationId(pickup != null ? pickup.getLocationId() : null)
                .pickupLocationName(pickup != null ? pickup.getLocationName() : null)
                .dropLocationId(drop != null ? drop.getLocationId() : null)
                .dropLocationName(drop != null ? drop.getLocationName() : null)
                .build();
    }
}
