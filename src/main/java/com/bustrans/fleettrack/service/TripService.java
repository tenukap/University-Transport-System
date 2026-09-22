package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.dto.TripResponseDTO;
import com.bustrans.fleettrack.entity.BusTrip;
import com.bustrans.fleettrack.repository.BusTripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {

    private final BusTripRepository busTripRepository;

    private static final Map<Integer, String> LOCATION_NAMES = Map.of(
            1, "SLIIT",
            2, "Kaduwela",
            3, "Malabe",
            4, "Colombo Fort",
            5, "Nugegoda",
            6, "Maharagama",
            7, "Kottawa",
            8, "Pannipitiya",
            9, "Battaramulla",
            10, "Rajagiriya"
    );

    public List<TripResponseDTO> getAllTrips() {
        return busTripRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<TripResponseDTO> getTripsByDate(LocalDate date) {
        return busTripRepository.findByTripDate(date).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public TripResponseDTO mapToDTO(BusTrip trip) {
        return TripResponseDTO.builder()
                .tripId(trip.getTripId())
                .tripDate(trip.getTripDate())
                .startTime(trip.getStartTime())
                .eta(trip.getEta())
                .pickupLocationId(trip.getPickupLocationId())
                .dropLocationId(trip.getDropLocationId())
                .pickupLocationName(LOCATION_NAMES.getOrDefault(trip.getPickupLocationId(), "Unknown"))
                .dropLocationName(LOCATION_NAMES.getOrDefault(trip.getDropLocationId(), "Unknown"))
                .build();
    }
}
