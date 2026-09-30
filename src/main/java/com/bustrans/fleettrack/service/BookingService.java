package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.dto.BookingRequestDTO;
import com.bustrans.fleettrack.dto.BookingResponseDTO;
import com.bustrans.fleettrack.dto.MonthlyChargesDTO;
import com.bustrans.fleettrack.entity.Booking;
import com.bustrans.fleettrack.entity.Bus;
import com.bustrans.fleettrack.entity.BusTrip;
import com.bustrans.fleettrack.entity.Location;
import com.bustrans.fleettrack.entity.SeatReservation;
import com.bustrans.fleettrack.entity.User;
import com.bustrans.fleettrack.repository.BookingRepository;
import com.bustrans.fleettrack.repository.BusTripRepository;
import com.bustrans.fleettrack.repository.FeedbackRepository;
import com.bustrans.fleettrack.repository.LocationRepository;
import com.bustrans.fleettrack.repository.SeatReservationRepository;
import com.bustrans.fleettrack.repository.StudentRepository;
import com.bustrans.fleettrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BusTripRepository busTripRepository;
    private final LocationRepository locationRepository;
    private final StudentRepository studentRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final FeedbackRepository feedbackRepository;

    @Value("${booking.fare:150.00}")
    private BigDecimal bookingFare;

    @Transactional
    public BookingResponseDTO createBooking(Long userId, BookingRequestDTO request) {
        if (!studentRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only students can create bookings");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Integer tripId = request.getTripId();
        // Pessimistic write lock serializes concurrent bookings on the same trip.
        BusTrip busTrip = busTripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + tripId));

        if (!"Scheduled".equalsIgnoreCase(busTrip.getTripStatus())) {
            throw new RuntimeException(
                "Trip is not available for booking (status: " + busTrip.getTripStatus() + ")");
        }

        LocalDate tripDate = busTrip.getTripDate();
        LocalTime startTime = busTrip.getStartTime();
        if (tripDate != null && startTime != null) {
            boolean departed = tripDate.isBefore(LocalDate.now())
                    || (tripDate.isEqual(LocalDate.now()) && !startTime.isAfter(LocalTime.now()));
            if (departed) {
                throw new RuntimeException(
                    "This trip has already departed (" + tripDate + " at " + startTime + ")");
            }
        }

        List<Booking> activeOnTrip = bookingRepository.findByBusTrip_TripIdAndStatusNot(tripId, "CANCELLED");

        boolean alreadyBooked = activeOnTrip.stream()
                .anyMatch(b -> userId.equals(b.getUser() != null ? b.getUser().getUserId() : null));
        if (alreadyBooked) {
            throw new RuntimeException("You already have a booking for this trip");
        }

        Bus bus = busTrip.getBus();
        if (bus == null) {
            throw new RuntimeException("Trip has no bus assigned");
        }
        int capacity = bus.getPassengerCapacity() != null ? bus.getPassengerCapacity() : 0;

        Set<Integer> usedSeats = activeOnTrip.stream()
                .map(Booking::getSeatNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Integer assignedSeat = null;
        for (int seat = 1; seat <= capacity; seat++) {
            if (!usedSeats.contains(seat)) {
                assignedSeat = seat;
                break;
            }
        }
        if (assignedSeat == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This trip is fully booked");
        }

        Booking booking = Booking.builder()
                .user(user)
                .busTrip(busTrip)
                .pickupLocation(resolveLocation(request.getPickupLocId()))
                .dropoffLocation(resolveLocation(request.getDropoffLocId()))
                .seatNumber(assignedSeat)
                .fareAmount(bookingFare)
                .status("CONFIRMED")
                .createdAt(LocalDateTime.now())
                .build();

        try {
            Booking saved = bookingRepository.saveAndFlush(booking);
            SeatReservation sr = SeatReservation.builder()
                    .booking(saved)
                    .bus(bus)
                    .seatNumber(assignedSeat)
                    .reservedAt(LocalDateTime.now())
                    .build();
            seatReservationRepository.save(sr);
            return mapToDTO(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "That seat was just taken, please try again");
        }
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

    @Transactional
    public BookingResponseDTO cancelBooking(Long id, Long callerId, boolean isAdmin) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));

        if (!isAdmin) {
            Long ownerId = booking.getUser() != null ? booking.getUser().getUserId() : null;
            if (!callerId.equals(ownerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only cancel your own bookings");
            }
        }

        String status = booking.getStatus();
        if (!"PENDING".equalsIgnoreCase(status) && !"CONFIRMED".equalsIgnoreCase(status)) {
            throw new RuntimeException("Only PENDING or CONFIRMED bookings can be cancelled");
        }

        booking.setStatus("CANCELLED");
        Booking saved = bookingRepository.save(booking);
        seatReservationRepository.deleteByBooking_Id(id);
        return mapToDTO(saved);
    }

    @Transactional
    public void deleteBooking(Long id, Long callerId, boolean isAdmin) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));

        if (!isAdmin) {
            Long ownerId = booking.getUser() != null ? booking.getUser().getUserId() : null;
            if (!callerId.equals(ownerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only delete your own bookings");
            }
        }

        if (!"CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            BusTrip trip = booking.getBusTrip();
            if (trip != null) {
                LocalDate tripDate = trip.getTripDate();
                LocalTime tripStart = trip.getStartTime();
                if (tripDate != null) {
                    boolean departed = tripDate.isBefore(LocalDate.now())
                            || (tripDate.isEqual(LocalDate.now())
                                && tripStart != null
                                && !tripStart.isAfter(LocalTime.now()));
                    if (departed) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Bookings for trips that have already departed cannot be deleted");
                    }
                }
            }
        }

        seatReservationRepository.deleteByBooking_Id(id);
        feedbackRepository.clearBookingId(id);
        bookingRepository.delete(booking);
    }

    public MonthlyChargesDTO getMonthlyCharges(Long userId, int month, int year) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate nextMonth = firstDay.plusMonths(1);

        List<Booking> chargeable = bookingRepository.findChargeableByUserAndMonth(userId, firstDay, nextMonth);

        BigDecimal total = chargeable.stream()
                .map(Booking::getFareAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        List<MonthlyChargesDTO.ChargeItem> items = chargeable.stream()
                .map(b -> {
                    BusTrip trip = b.getBusTrip();
                    return MonthlyChargesDTO.ChargeItem.builder()
                            .bookingId(b.getId())
                            .tripDate(trip != null && trip.getTripDate() != null
                                ? trip.getTripDate().toString() : null)
                            .pickup(b.getPickupLocation() != null
                                ? b.getPickupLocation().getLocationName() : null)
                            .dropoff(b.getDropoffLocation() != null
                                ? b.getDropoffLocation().getLocationName() : null)
                            .seatNumber(b.getSeatNumber())
                            .fareAmount(b.getFareAmount())
                            .status(b.getStatus())
                            .build();
                })
                .collect(Collectors.toList());

        // Last 6 months history, oldest first.
        LocalDate now = LocalDate.now();
        List<MonthlyChargesDTO.MonthSummary> months = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate m = now.minusMonths(i).withDayOfMonth(1);
            LocalDate mNext = m.plusMonths(1);
            BigDecimal mTotal = bookingRepository
                    .findChargeableByUserAndMonth(userId, m, mNext)
                    .stream()
                    .map(Booking::getFareAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            months.add(MonthlyChargesDTO.MonthSummary.builder()
                    .month(m.getMonthValue())
                    .year(m.getYear())
                    .total(mTotal)
                    .build());
        }

        return MonthlyChargesDTO.builder()
                .month(month)
                .year(year)
                .total(total)
                .count(items.size())
                .items(items)
                .months(months)
                .build();
    }

    private Location resolveLocation(Integer locationId) {
        if (locationId == null) return null;
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Location " + locationId + " was not found"));
    }

    private BookingResponseDTO mapToDTO(Booking booking) {
        return BookingResponseDTO.builder()
                .id(booking.getId())
                .userId(booking.getUser() != null ? booking.getUser().getUserId() : null)
                .tripId(booking.getBusTrip() != null ? booking.getBusTrip().getTripId() : null)
                .pickupLocId(booking.getPickupLocation() != null
                    ? booking.getPickupLocation().getLocationId() : null)
                .dropoffLocId(booking.getDropoffLocation() != null
                    ? booking.getDropoffLocation().getLocationId() : null)
                .seatNumber(booking.getSeatNumber())
                .fareAmount(booking.getFareAmount())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .tripDate(booking.getBusTrip() != null ? booking.getBusTrip().getTripDate() : null)
                .startTime(booking.getBusTrip() != null ? booking.getBusTrip().getStartTime() : null)
                .pickupName(booking.getPickupLocation() != null
                    ? booking.getPickupLocation().getLocationName() : null)
                .dropoffName(booking.getDropoffLocation() != null
                    ? booking.getDropoffLocation().getLocationName() : null)
                .build();
    }
}
