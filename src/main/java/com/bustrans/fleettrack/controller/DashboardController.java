package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.dto.BookingResponseDTO;
import com.bustrans.fleettrack.dto.UserDto.UserResponse;
import com.bustrans.fleettrack.service.BookingService;
import com.bustrans.fleettrack.service.FleetTrackService;
import com.bustrans.fleettrack.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DashboardController {

    private final FleetTrackService fleetTrackService;
    private final UserService userService;
    private final BookingService bookingService;

    @GetMapping
    public Map<String, Object> getDashboardSummary() {
        return fleetTrackService.getDashboardSummary();
    }

    @GetMapping("/users")
    public List<UserResponse> getUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/bookings")
    public List<BookingResponseDTO> getBookings() {
        return bookingService.getAllBookings();
    }
}
