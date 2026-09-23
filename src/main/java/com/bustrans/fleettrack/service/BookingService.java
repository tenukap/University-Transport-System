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
                .orElseThrow(() -> new IllegalArgumentException("User " + userId + " was not found"));

        BusTrip busTrip = busTripRepository.findById(request.getTripId())
                .orElseThrow(() -> new IllegalArgumentException("Trip " + request.getTripId() + " was not found"));

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
                .orElseThrow(() -> new IllegalArgumentException("Booking " + id + " was not found"));
        return mapToDTO(booking);
    }

    public BookingResponseDTO cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking " + id + " was not found"));
        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Booking already cancelled");
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
                .build();
    }
}
