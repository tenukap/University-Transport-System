package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.dto.BookingRequestDTO;
import com.bustrans.fleettrack.dto.BookingResponseDTO;
import com.bustrans.fleettrack.entity.Booking;
import com.bustrans.fleettrack.entity.BusTrip;
import com.bustrans.fleettrack.entity.Location;
import com.bustrans.fleettrack.entity.User;
import com.bustrans.fleettrack.repository.BookingRepository;
import com.bustrans.fleettrack.repository.BusTripRepository;
import com.bustrans.fleettrack.repository.LocationRepository;
import com.bustrans.fleettrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BusTripRepository busTripRepository;
    // Needed to resolve the pickup/dropoff location FK relations on Booking.
    private final LocationRepository locationRepository;

    public BookingResponseDTO createBooking(Long userId, BookingRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Integer tripId = request.getTripId();
        BusTrip busTrip = busTripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + tripId));

        if (!"Scheduled".equalsIgnoreCase(busTrip.getTripStatus())) {
            throw new RuntimeException("Trip is not available for booking");
        }

        boolean alreadyBooked = bookingRepository.findByUser_UserId(userId).stream()
                .anyMatch(b -> b.getBusTrip() != null
                        && tripId.equals(b.getBusTrip().getTripId())
                        && ("PENDING".equalsIgnoreCase(b.getStatus())
                            || "CONFIRMED".equalsIgnoreCase(b.getStatus())));
        if (alreadyBooked) {
            throw new RuntimeException("You already have a booking for this trip");
        }

        Booking booking = Booking.builder()
                .user(user)
                .busTrip(busTrip)
                .pickupLocation(resolveLocation(request.getPickupLocId()))
                .dropoffLocation(resolveLocation(request.getDropoffLocId()))
                .seatNumber(request.getSeatNumber())
                .fareAmount(request.getFareAmount())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        return mapToDTO(bookingRepository.save(booking));
    }

    public List<BookingResponseDTO> getBookingsByUser(Long userId) {
        return bookingRepository.findByUser_UserId(userId).stream()
                .map(this::mapToDTO)
                .toList();
    }

    public List<BookingResponseDTO> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    public BookingResponseDTO getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
        return mapToDTO(booking);
    }

    public BookingResponseDTO cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Booking is already cancelled");
        }
        booking.setStatus("CANCELLED");
        return mapToDTO(bookingRepository.save(booking));
    }

    /** A managed reference for the given location id, or null when no id supplied. */
    private Location resolveLocation(Integer locationId) {
        if (locationId == null) {
            return null;
        }
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Location " + locationId + " was not found"));
    }

    private BookingResponseDTO mapToDTO(Booking booking) {
        return BookingResponseDTO.builder()
                .id(booking.getId())
                .userId(booking.getUser() != null ? booking.getUser().getUserId() : null)
                .tripId(booking.getBusTrip() != null ? booking.getBusTrip().getTripId() : null)
                .pickupLocId(booking.getPickupLocation() != null ? booking.getPickupLocation().getLocationId() : null)
                .dropoffLocId(booking.getDropoffLocation() != null ? booking.getDropoffLocation().getLocationId() : null)
                .seatNumber(booking.getSeatNumber())
                .fareAmount(booking.getFareAmount())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .tripDate(booking.getBusTrip() != null ? booking.getBusTrip().getTripDate() : null)
                .startTime(booking.getBusTrip() != null ? booking.getBusTrip().getStartTime() : null)
                .pickupName(booking.getPickupLocation() != null ? booking.getPickupLocation().getLocationName() : null)
                .dropoffName(booking.getDropoffLocation() != null ? booking.getDropoffLocation().getLocationName() : null)
                .build();
    }
}
