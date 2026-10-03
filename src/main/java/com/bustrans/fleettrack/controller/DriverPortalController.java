package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.dto.DriverTripDTO;
import com.bustrans.fleettrack.entity.BusTrip;
import com.bustrans.fleettrack.entity.TripStatus;
import com.bustrans.fleettrack.repository.BusTripRepository;
import com.bustrans.fleettrack.repository.TripStatusRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/driver")
@CrossOrigin(origins = "*")
public class DriverPortalController {

    private final BusTripRepository busTripRepository;
    private final TripStatusRepository tripStatusRepository;

    public DriverPortalController(BusTripRepository busTripRepository,
                                  TripStatusRepository tripStatusRepository) {
        this.busTripRepository = busTripRepository;
        this.tripStatusRepository = tripStatusRepository;
    }

    @GetMapping("/my-trips")
    public List<DriverTripDTO> getMyTrips(Authentication auth) {
        Integer driverId = Integer.parseInt(auth.getName());
        List<BusTrip> trips = busTripRepository.findUpcomingByDriver(driverId, LocalDate.now());
        return trips.stream().map(trip -> {
            String latestStatus = tripStatusRepository
                    .findTopByTripIdOrderByUpdatedAtDesc(trip.getTripId())
                    .map(TripStatus::getStatusType)
                    .orElse(null);
            return new DriverTripDTO(
                    trip.getTripId(),
                    trip.getTripDate() != null ? trip.getTripDate().toString() : null,
                    trip.getStartTime() != null ? trip.getStartTime().toString() : null,
                    trip.getEta() != null ? trip.getEta().toString() : null,
                    trip.getPickupLocation() != null ? trip.getPickupLocation().getLocationName() : null,
                    trip.getDropLocation() != null ? trip.getDropLocation().getLocationName() : null,
                    trip.getBus() != null ? trip.getBus().getRegistrationNumber() : null,
                    trip.getTripStatus(),
                    latestStatus
            );
        }).collect(Collectors.toList());
    }
}
