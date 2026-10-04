package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.dto.*;
import com.bustrans.fleettrack.entity.*;
import com.bustrans.fleettrack.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transport/tracking")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('TRANSPORT_OFFICER', 'ADMIN')")
public class TrackingController {

    @Autowired private BusTripRepository busTripRepo;
    @Autowired private TripStatusRepository tripStatusRepo;
    @Autowired private LocationUpdateRepository locationUpdateRepo;
    @Autowired private BusRepository busRepo;
    @Autowired private UserRepository userRepo;

    /** Trips from 2 days ago to 30 days ahead, excluding Cancelled, newest first. */
    @GetMapping("/trips")
    public Map<String, Object> getTrackingTrips() {
        LocalDate from = LocalDate.now().minusDays(2);
        LocalDate to   = LocalDate.now().plusDays(30);
        List<BusTrip> trips = busTripRepo.findTrackingTrips(from, to, "Cancelled");
        List<Integer> tripIds = trips.stream().map(BusTrip::getTripId).collect(Collectors.toList());

        Map<Integer, TripStatus>     latestStatus = batchLatestStatus(tripIds);
        Map<Integer, LocationUpdate> latestPing   = batchLatestPing(tripIds);
        Map<Long, String>            driverNames  = batchDriverNames(trips);

        List<TrackingTripDTO> data = trips.stream()
                .map(t -> toTripDTO(t, latestStatus.get(t.getTripId()), latestPing.get(t.getTripId()), driverNames))
                .collect(Collectors.toList());

        return wrap(data);
    }

    /** One row per bus: registration, status, today's trip count, current/next trip with latest ping. */
    @GetMapping("/buses")
    public Map<String, Object> getTrackingBuses() {
        LocalDate today = LocalDate.now();
        LocalDate from  = today.minusDays(2);
        LocalDate to    = today.plusDays(30);

        List<Bus>     buses = busRepo.findAll();
        List<BusTrip> trips = busTripRepo.findTrackingTrips(from, to, "Cancelled");

        Map<Long, List<BusTrip>> tripsByBus = trips.stream()
                .filter(t -> t.getBus() != null)
                .collect(Collectors.groupingBy(t -> t.getBus().getBusId()));

        List<Integer> allIds = trips.stream().map(BusTrip::getTripId).collect(Collectors.toList());
        Map<Integer, TripStatus>     latestStatus = batchLatestStatus(allIds);
        Map<Integer, LocationUpdate> latestPing   = batchLatestPing(allIds);
        Map<Long, String>            driverNames  = batchDriverNames(trips);

        LocalTime now = LocalTime.now();
        List<TrackingBusDTO> data = buses.stream().map(bus -> {
            List<BusTrip> busTrips = tripsByBus.getOrDefault(bus.getBusId(), List.of());
            long todayCount = busTrips.stream().filter(t -> today.equals(t.getTripDate())).count();
            BusTrip cur = findCurrentOrNext(busTrips, today, now);
            TrackingTripDTO curDTO = cur == null ? null :
                    toTripDTO(cur, latestStatus.get(cur.getTripId()), latestPing.get(cur.getTripId()), driverNames);
            return new TrackingBusDTO(bus.getBusId(), bus.getRegistrationNumber(),
                    bus.getPassengerCapacity(), bus.getStatus(), curDTO, (int) todayCount);
        }).collect(Collectors.toList());

        return wrap(data);
    }

    /** Bus detail: trips for one bus from 7 days ago to 30 days ahead, each with full status log and last 10 pings. */
    @GetMapping("/buses/{id}")
    public Map<String, Object> getBusDetail(@PathVariable Long id) {
        Bus bus = busRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bus not found: " + id));

        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to   = LocalDate.now().plusDays(30);
        List<BusTrip> trips = busTripRepo.findByBusIdAndDateRange(id, from, to);
        List<Integer> tripIds = trips.stream().map(BusTrip::getTripId).collect(Collectors.toList());

        // All status rows for history view
        Map<Integer, List<TripStatus>> statusHistory = tripIds.isEmpty() ? Map.of() :
                tripStatusRepo.findByTripIdIn(tripIds).stream()
                        .collect(Collectors.groupingBy(TripStatus::getTripId));

        // All pings newest-first; take top 10 per trip in Java
        Map<Integer, List<LocationUpdate>> pingsByTrip = tripIds.isEmpty() ? Map.of() :
                locationUpdateRepo.findByTripIdsOrderByRecordedAtDesc(tripIds).stream()
                        .collect(Collectors.groupingBy(LocationUpdate::getTripId));

        List<TrackingBusDetailDTO.TripWithHistory> histories = trips.stream().map(t -> {
            List<TripStatus> statuses = new ArrayList<>(statusHistory.getOrDefault(t.getTripId(), List.of()));
            statuses.sort(Comparator.comparing(TripStatus::getUpdatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            List<TrackingBusDetailDTO.StatusEntry> sLog = statuses.stream()
                    .map(s -> new TrackingBusDetailDTO.StatusEntry(
                            s.getStatusType(),
                            s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null))
                    .collect(Collectors.toList());

            List<LocationUpdate> pings = pingsByTrip.getOrDefault(t.getTripId(), List.of());
            List<TrackingBusDetailDTO.PingEntry> pLog = pings.stream().limit(10)
                    .map(p -> new TrackingBusDetailDTO.PingEntry(
                            p.getLatitude()  != null ? p.getLatitude().toPlainString()  : null,
                            p.getLongitude() != null ? p.getLongitude().toPlainString() : null,
                            p.getRecordedAt() != null ? p.getRecordedAt().toString()    : null))
                    .collect(Collectors.toList());

            return new TrackingBusDetailDTO.TripWithHistory(
                    t.getTripId(),
                    t.getTripDate()       != null ? t.getTripDate().toString()       : null,
                    t.getStartTime()      != null ? t.getStartTime().toString()      : null,
                    t.getEta()            != null ? t.getEta().toString()            : null,
                    t.getPickupLocation() != null ? t.getPickupLocation().getLocationName() : null,
                    t.getDropLocation()   != null ? t.getDropLocation().getLocationName()   : null,
                    t.getTripStatus(), sLog, pLog);
        }).collect(Collectors.toList());

        return wrap(new TrackingBusDetailDTO(bus.getBusId(), bus.getRegistrationNumber(),
                bus.getPassengerCapacity(), bus.getStatus(), histories));
    }

    // ---- helpers ----

    /** Prefer a trip running right now (In Progress or within start–ETA window); fall back to next upcoming. */
    private BusTrip findCurrentOrNext(List<BusTrip> trips, LocalDate today, LocalTime now) {
        Optional<BusTrip> current = trips.stream()
                .filter(t -> today.equals(t.getTripDate()))
                .filter(t -> "In Progress".equals(t.getTripStatus())
                          || (t.getStartTime() != null && t.getEta() != null
                              && !t.getStartTime().isAfter(now) && !t.getEta().isBefore(now)))
                .findFirst();
        if (current.isPresent()) return current.get();

        return trips.stream()
                .filter(t -> t.getTripDate() != null)
                .filter(t -> t.getTripDate().isAfter(today)
                          || (today.equals(t.getTripDate())
                              && t.getStartTime() != null && t.getStartTime().isAfter(now)))
                .min(Comparator.comparing(BusTrip::getTripDate)
                        .thenComparing(t -> t.getStartTime() != null ? t.getStartTime() : LocalTime.MAX))
                .orElse(null);
    }

    private TrackingTripDTO toTripDTO(BusTrip t, TripStatus s, LocationUpdate p,
                                      Map<Long, String> driverNames) {
        Bus bus = t.getBus();
        String driverName = t.getDriverUserId() != null
                ? driverNames.getOrDefault(t.getDriverUserId().longValue(), "Unknown") : null;
        return new TrackingTripDTO(
                t.getTripId(),
                t.getTripDate()       != null ? t.getTripDate().toString()       : null,
                t.getStartTime()      != null ? t.getStartTime().toString()      : null,
                t.getEta()            != null ? t.getEta().toString()            : null,
                t.getPickupLocation() != null ? t.getPickupLocation().getLocationName() : null,
                t.getDropLocation()   != null ? t.getDropLocation().getLocationName()   : null,
                bus != null ? bus.getBusId()              : null,
                bus != null ? bus.getRegistrationNumber() : null,
                driverName,
                t.getTripStatus(),
                s != null ? s.getStatusType()                                         : null,
                s != null && s.getUpdatedAt()  != null ? s.getUpdatedAt().toString()  : null,
                p != null && p.getLatitude()   != null ? p.getLatitude().toPlainString()  : null,
                p != null && p.getLongitude()  != null ? p.getLongitude().toPlainString() : null,
                p != null && p.getRecordedAt() != null ? p.getRecordedAt().toString()     : null);
    }

    private Map<Integer, TripStatus> batchLatestStatus(List<Integer> tripIds) {
        if (tripIds.isEmpty()) return Map.of();
        return tripStatusRepo.findLatestByTripIds(tripIds).stream()
                .collect(Collectors.toMap(TripStatus::getTripId, s -> s));
    }

    private Map<Integer, LocationUpdate> batchLatestPing(List<Integer> tripIds) {
        if (tripIds.isEmpty()) return Map.of();
        return locationUpdateRepo.findLatestByTripIds(tripIds).stream()
                .collect(Collectors.toMap(LocationUpdate::getTripId, l -> l));
    }

    private Map<Long, String> batchDriverNames(List<BusTrip> trips) {
        Set<Long> ids = trips.stream()
                .filter(t -> t.getDriverUserId() != null)
                .map(t -> t.getDriverUserId().longValue())
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return userRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getUserId, User::getFullName));
    }

    private Map<String, Object> wrap(Object data) {
        Map<String, Object> m = new HashMap<>();
        m.put("serverTime", LocalDateTime.now().toString());
        m.put("data", data);
        return m;
    }
}
