package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.Booking;
import com.bustrans.fleettrack.repository.BookingRepository;
import com.bustrans.fleettrack.repository.BusTripRepository;
import com.bustrans.fleettrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FleetTrackService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final BusTripRepository busTripRepository;

    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalUsers", userRepository.count());
        summary.put("activeBookings", bookingRepository.countByStatus("PENDING"));
        summary.put("upcomingTrips", busTripRepository.findByTripStatus("Scheduled").size());
        summary.put("activeUsers", userRepository.countByAccountStatus("Active"));
        return summary;
    }

    /**
     * Financial summary of bookings. When both dates are provided (yyyy-MM-dd) the
     * range is applied to created_at; otherwise all bookings are summarised.
     */
    public Map<String, Object> getFinancialReport(String startDate, String endDate) {
        List<Booking> bookings;
        if (startDate != null && !startDate.isBlank() && endDate != null && !endDate.isBlank()) {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            bookings = bookingRepository.findByCreatedAtBetween(
                    start.atStartOfDay(), end.atTime(LocalTime.MAX));
        } else {
            bookings = bookingRepository.findAll();
        }

        BigDecimal grossRevenue = bookings.stream()
                .map(Booking::getFareAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long pending = bookings.stream()
                .filter(b -> "PENDING".equalsIgnoreCase(b.getStatus()))
                .count();
        long cancelled = bookings.stream()
                .filter(b -> "CANCELLED".equalsIgnoreCase(b.getStatus()))
                .count();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalBookings", bookings.size());
        report.put("grossRevenue", grossRevenue);
        report.put("pendingBookings", pending);
        report.put("cancelledBookings", cancelled);
        return report;
    }
}
