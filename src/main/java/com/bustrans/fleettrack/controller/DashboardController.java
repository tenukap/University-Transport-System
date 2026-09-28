package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.dto.BookingResponseDTO;
import com.bustrans.fleettrack.dto.UserDto.UserResponse;
import com.bustrans.fleettrack.service.BookingService;
import com.bustrans.fleettrack.service.FleetTrackService;
import com.bustrans.fleettrack.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Admin-only. Paths are declared per-method so the reports endpoint can live
// outside the /api/dashboard prefix. getFinancialReport also allows FINANCE_OFFICER.
// Class-level @PreAuthorize is intentionally absent: Spring Security 6.3 evaluates
// both class-level and method-level annotations, so per-method annotations are used here.
@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final FleetTrackService fleetTrackService;
    private final UserService userService;
    private final BookingService bookingService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/dashboard")
    public Map<String, Object> getDashboardSummary() {
        return fleetTrackService.getDashboardSummary();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/dashboard/users")
    public List<UserResponse> getUsers() {
        return userService.getAllUsers();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/dashboard/bookings")
    public List<BookingResponseDTO> getBookings() {
        return bookingService.getAllBookings();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_OFFICER')")
    @GetMapping("/api/reports/financial")
    public Map<String, Object> getFinancialReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return fleetTrackService.getFinancialReport(startDate, endDate);
    }
}
