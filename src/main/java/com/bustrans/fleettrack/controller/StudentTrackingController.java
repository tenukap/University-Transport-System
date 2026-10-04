package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.dto.StudentTrackingDTO;
import com.bustrans.fleettrack.entity.*;
import com.bustrans.fleettrack.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student/tracking")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('STUDENT')")
public class StudentTrackingController {

    @Autowired private BookingRepository bookingRepo;
    @Autowired private TripStatusRepository tripStatusRepo;
    @Autowired private LocationUpdateRepository locationUpdateRepo;

    /** Active bookings for trips today-1 to +14 days; userId comes from the JWT sub claim only. */
    @GetMapping
    public Map<String, Object> getMyTracking(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());

        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to   = LocalDate.now().plusDays(14);
        List<Booking> bookings = bookingRepo.findActiveBookingsForTracking(userId, from, to);

        List<Integer> tripIds = bookings.stream()
                .map(b -> b.getBusTrip().getTripId()).distinct().collect(Collectors.toList());

        Map<Integer, TripStatus> latestStatus = tripIds.isEmpty() ? Map.of() :
                tripStatusRepo.findLatestByTripIds(tripIds).stream()
                        .collect(Collectors.toMap(TripStatus::getTripId, s -> s));

        Map<Integer, LocationUpdate> latestPing = tripIds.isEmpty() ? Map.of() :
                locationUpdateRepo.findLatestByTripIds(tripIds).stream()
                        .collect(Collectors.toMap(LocationUpdate::getTripId, l -> l));

        List<StudentTrackingDTO> data = bookings.stream().map(b -> {
            BusTrip trip = b.getBusTrip();
            Bus bus = trip.getBus();
            TripStatus     s = latestStatus.get(trip.getTripId());
            LocationUpdate p = latestPing.get(trip.getTripId());
            return new StudentTrackingDTO(
                    trip.getTripId(),
                    trip.getTripDate()       != null ? trip.getTripDate().toString()   : null,
                    trip.getStartTime()      != null ? trip.getStartTime().toString()  : null,
                    trip.getEta()            != null ? trip.getEta().toString()        : null,
                    trip.getPickupLocation() != null ? trip.getPickupLocation().getLocationName() : null,
                    trip.getDropLocation()   != null ? trip.getDropLocation().getLocationName()   : null,
                    bus != null ? bus.getRegistrationNumber() : null,
                    b.getSeatNumber(),
                    trip.getTripStatus(),
                    s != null ? s.getStatusType() : null,
                    s != null && s.getUpdatedAt()  != null ? s.getUpdatedAt().toString()  : null,
                    p != null && p.getLatitude()   != null ? p.getLatitude().toPlainString()  : null,
                    p != null && p.getLongitude()  != null ? p.getLongitude().toPlainString() : null,
                    p != null && p.getRecordedAt() != null ? p.getRecordedAt().toString()     : null);
        }).collect(Collectors.toList());

        Map<String, Object> resp = new HashMap<>();
        resp.put("serverTime", LocalDateTime.now().toString());
        resp.put("data", data);
        return resp;
    }
}
