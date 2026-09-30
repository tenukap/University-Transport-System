package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.dto.BookingRequestDTO;
import com.bustrans.fleettrack.dto.BookingResponseDTO;
import com.bustrans.fleettrack.dto.MonthlyChargesDTO;
import com.bustrans.fleettrack.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(@RequestBody BookingRequestDTO request,
                                                             Authentication authentication) {
        BookingResponseDTO response = bookingService.createBooking(userId(authentication), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Monthly travel charges for the authenticated student.
     * Mapped before /{id:\\d+} to prevent any ambiguity.
     */
    @GetMapping("/charges")
    public MonthlyChargesDTO getMyCharges(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Authentication authentication) {
        Long userId = userId(authentication);
        LocalDate now = LocalDate.now();
        int m = (month != null) ? month : now.getMonthValue();
        int y = (year != null) ? year : now.getYear();
        if (m < 1 || m > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "month must be between 1 and 12");
        }
        if (y < 2020 || y > now.getYear() + 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid year");
        }
        return bookingService.getMonthlyCharges(userId, m, y);
    }

    @GetMapping("/user/{userId}")
    public List<BookingResponseDTO> getBookingsByUser(@PathVariable Long userId,
                                                       Authentication authentication) {
        Long callerId = userId(authentication);
        if (!isAdmin(authentication) && !callerId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You can only view your own bookings");
        }
        return bookingService.getBookingsByUser(userId);
    }

    @GetMapping("/{id:\\d+}")
    public BookingResponseDTO getBookingById(@PathVariable Long id, Authentication authentication) {
        BookingResponseDTO booking = bookingService.getBookingById(id);
        Long callerId = userId(authentication);
        if (!isAdmin(authentication) && !callerId.equals(booking.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You can only view your own bookings");
        }
        return booking;
    }

    @PutMapping("/{id:\\d+}/cancel")
    public BookingResponseDTO cancelBooking(@PathVariable Long id, Authentication authentication) {
        return bookingService.cancelBooking(id, userId(authentication), isAdmin(authentication));
    }

    @DeleteMapping("/{id:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBooking(@PathVariable Long id, Authentication authentication) {
        bookingService.deleteBooking(id, userId(authentication), isAdmin(authentication));
    }

    // ---- helpers ----

    private Long userId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        try {
            return Long.parseLong(auth.getPrincipal().toString());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token");
        }
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
