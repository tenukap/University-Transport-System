package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.Bus;
import com.bustrans.fleettrack.repository.BookingRepository;
import com.bustrans.fleettrack.repository.BusRepository;
import com.bustrans.fleettrack.repository.BusTripRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class BusService {

    @Autowired private BusRepository busRepository;
    @Autowired private BusTripRepository busTripRepository;
    @Autowired private BookingRepository bookingRepository;

    /** Validates and saves a new bus. Duplicate registration → 409 via GlobalExceptionHandler. */
    public Bus addBus(String registrationNumber, Integer capacity) {
        if (registrationNumber == null || registrationNumber.isBlank()) {
            throw new RuntimeException("Registration number is required");
        }
        if (capacity == null || capacity < 1 || capacity > 100) {
            throw new RuntimeException("Capacity must be between 1 and 100");
        }
        String reg = registrationNumber.trim().toUpperCase();
        // "already exists" in the message makes GlobalExceptionHandler return 409.
        if (busRepository.existsByRegistrationNumber(reg)) {
            throw new RuntimeException("Bus " + reg + " already exists");
        }
        Bus bus = new Bus();
        bus.setRegistrationNumber(reg);
        bus.setPassengerCapacity(capacity);
        bus.setStatus("Available");
        return busRepository.save(bus);
    }

    /** Edits registration and/or capacity of an existing bus. */
    public Bus updateBus(Long id, String registrationNumber, Integer capacity) {
        Bus existing = busRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bus not found: " + id));

        if (registrationNumber != null && !registrationNumber.isBlank()) {
            String reg = registrationNumber.trim().toUpperCase();
            if (busRepository.existsByRegistrationNumberAndBusIdNot(reg, id)) {
                throw new RuntimeException("Bus " + reg + " already exists");
            }
            existing.setRegistrationNumber(reg);
        }

        if (capacity != null) {
            if (capacity < 1 || capacity > 100) {
                throw new RuntimeException("Capacity must be between 1 and 100");
            }
            // Guard: new capacity must not be less than the max active bookings on any upcoming trip.
            int maxBooked = maxActiveBookingsOnUpcomingTrips(id);
            if (capacity < maxBooked) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cannot reduce capacity to " + capacity + ": " + maxBooked
                        + " active booking(s) exist on an upcoming trip of this bus");
            }
            existing.setPassengerCapacity(capacity);
        }
        return busRepository.save(existing);
    }

    /**
     * Returns the highest count of non-CANCELLED bookings across all upcoming non-cancelled trips
     * for this bus. Zero if the bus has no such trips.
     */
    private int maxActiveBookingsOnUpcomingTrips(Long busId) {
        return busTripRepository
                .findByBus_BusIdAndTripDateGreaterThanEqualAndTripStatusNot(busId, LocalDate.now(), "Cancelled")
                .stream()
                .mapToInt(t -> bookingRepository.findByBusTrip_TripIdAndStatusNot(t.getTripId(), "CANCELLED").size())
                .max()
                .orElse(0);
    }

    /** Changes bus status. Moving away from 'Available' while upcoming trips exist → 409. */
    public Bus setStatus(Long id, String newStatus) {
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bus not found: " + id));

        List<String> allowed = List.of("Available", "Maintenance", "Out of Service");
        if (!allowed.contains(newStatus)) {
            throw new RuntimeException("Status must be Available, Maintenance, or Out of Service");
        }

        if (!"Available".equals(newStatus)) {
            long upcoming = busTripRepository
                    .findByBus_BusIdAndTripDateGreaterThanEqualAndTripStatusNot(id, LocalDate.now(), "Cancelled")
                    .size();
            if (upcoming > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cannot change status: " + upcoming
                        + " upcoming trip(s) are assigned to this bus. Reassign them first.");
            }
        }
        bus.setStatus(newStatus);
        return busRepository.save(bus);
    }

    /** Hard-deletes a bus. Blocked if the bus has any trip history at all. */
    public void deleteBus(Long id) {
        if (!busRepository.existsById(id)) {
            throw new RuntimeException("Bus not found: " + id);
        }
        if (busTripRepository.existsByBus_BusId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This bus has trip history. Set it to Out of Service instead.");
        }
        busRepository.deleteById(id);
    }

    public List<Bus> getAllBuses() {
        return busRepository.findAll();
    }

    public Bus getBusById(Long id) {
        return busRepository.findById(id).orElse(null);
    }
}
