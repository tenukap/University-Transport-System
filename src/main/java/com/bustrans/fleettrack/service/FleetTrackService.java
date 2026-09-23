package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.repository.BookingRepository;
import com.bustrans.fleettrack.repository.BusTripRepository;
import com.bustrans.fleettrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

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
}
