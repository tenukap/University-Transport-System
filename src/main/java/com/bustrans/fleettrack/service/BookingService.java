package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.dto.BookingRequestDTO;
import com.bustrans.fleettrack.dto.BookingResponseDTO;
import com.bustrans.fleettrack.entity.Booking;
import com.bustrans.fleettrack.entity.Bus;
import com.bustrans.fleettrack.entity.SeatReservation;
import com.bustrans.fleettrack.entity.Student;
import com.bustrans.fleettrack.repository.BookingRepository;
import com.bustrans.fleettrack.repository.BusRepository;
import com.bustrans.fleettrack.repository.SeatReservationRepository;
import com.bustrans.fleettrack.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final StudentRepository studentRepository;
    private final BusRepository busRepository;

    public BookingResponseDTO createBooking(BookingRequestDTO request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Booking booking = Booking.builder()
                .student(student)
                .tripId(request.getTripId())
                .pickupLocId(request.getPickupLocId())
                .dropoffLocId(request.getDropoffLocId())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        booking = bookingRepository.save(booking);

        // Bus is hardcoded to id = 1 for now until the bus-trip relationship is confirmed.
        Bus bus = busRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Bus not found"));

        long reservedCount = seatReservationRepository
                .countByBusIdAndBookingTripId(bus.getBusId(), request.getTripId());
        int capacity = bus.getPassengerCapacity();
        if (reservedCount >= capacity) {
            throw new RuntimeException("Bus is fully booked");
        }

        int nextSeatNumber = seatReservationRepository
                .findMaxSeatNumberByBusIdAndBookingTripId(bus.getBusId(), request.getTripId())
                .map(max -> max + 1)
                .orElse(1);

        SeatReservation seatReservation = SeatReservation.builder()
                .booking(booking)
                .bus(bus)
                .seatNumber(nextSeatNumber)
                .reservedAt(LocalDateTime.now())
                .build();
        seatReservation = seatReservationRepository.save(seatReservation);

        booking.setStatus("CONFIRMED");
        booking = bookingRepository.save(booking);

        return mapToResponseDTO(booking, Optional.of(seatReservation));
    }

    public List<BookingResponseDTO> getBookingsByStudent(Long studentId) {
        return bookingRepository.findByStudentId(studentId).stream()
                .map(booking -> mapToResponseDTO(booking,
                        seatReservationRepository.findByBookingId(booking.getId())))
                .toList();
    }

    public BookingResponseDTO getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        Optional<SeatReservation> seatReservation =
                seatReservationRepository.findByBookingId(bookingId);
        return mapToResponseDTO(booking, seatReservation);
    }

    public BookingResponseDTO cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        if ("CANCELLED".equals(booking.getStatus())) {
            throw new RuntimeException("Booking already cancelled");
        }
        booking.setStatus("CANCELLED");
        booking = bookingRepository.save(booking);
        return mapToResponseDTO(booking, seatReservationRepository.findByBookingId(bookingId));
    }

    private BookingResponseDTO mapToResponseDTO(Booking booking,
                                                Optional<SeatReservation> seatReservation) {
        return BookingResponseDTO.builder()
                .id(booking.getId())
                .studentId(booking.getStudent().getId())
                .studentName(booking.getStudent().getFullName())
                .tripId(booking.getTripId())
                .pickupLocId(booking.getPickupLocId())
                .dropoffLocId(booking.getDropoffLocId())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .seatNumber(seatReservation.map(SeatReservation::getSeatNumber).orElse(null))
                .build();
    }
}
