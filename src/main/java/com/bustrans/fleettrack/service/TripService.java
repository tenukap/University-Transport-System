package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.dto.TripResponseDTO;
import com.bustrans.fleettrack.dto.TripSeatsDTO;
import com.bustrans.fleettrack.entity.Bus;
import com.bustrans.fleettrack.entity.BusTrip;
import com.bustrans.fleettrack.entity.Location;
import com.bustrans.fleettrack.repository.BookingRepository;
import com.bustrans.fleettrack.repository.BusTripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {

    private final BusTripRepository busTripRepository;
    private final BookingRepository bookingRepository;

    public List<TripResponseDTO> getAllTrips() {
        return busTripRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<TripResponseDTO> getAvailableTrips() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        return busTripRepository
                .findAvailableTrips(today)
                .stream()
                // DB returns all Scheduled trips from today onwards; filter out same-day
                // trips that have already departed. LocalTime comparison is safe in Java.
                .filter(bt -> {
                    if (bt.getTripDate() == null) return false;
                    if (bt.getTripDate().isAfter(today)) return true;
                    return bt.getTripDate().isEqual(today)
                            && bt.getStartTime() != null
                            && bt.getStartTime().isAfter(now);
                })
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public TripResponseDTO getTripById(Integer id) {
        BusTrip trip = busTripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));
        return mapToDTO(trip);
    }

    public TripSeatsDTO getTripSeats(Integer tripId) {
        BusTrip trip = busTripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + tripId));
        Bus bus = trip.getBus();
        int capacity = (bus != null && bus.getPassengerCapacity() != null)
            ? bus.getPassengerCapacity() : 0;
        int booked = bookingRepository.findByBusTrip_TripIdAndStatusNot(tripId, "CANCELLED").size();
        int available = Math.max(0, capacity - booked);
        return new TripSeatsDTO(capacity, booked, available);
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
