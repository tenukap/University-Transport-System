package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.dto.*;
import com.bustrans.fleettrack.entity.*;
import com.bustrans.fleettrack.repository.*;
import com.bustrans.fleettrack.service.PaymentService;
import com.bustrans.fleettrack.service.RouteService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transport")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('TRANSPORT_OFFICER', 'ADMIN')")
public class TransportController {

    @Autowired private RouteService routeService;
    @Autowired private BusTripRepository busTripRepo;
    @Autowired private TripCancellationRepository tripCancelRepo;
    @Autowired private PaymentService paymentService;
    @Autowired private PaymentCancellationRepository paymentCancelRepo;
    @Autowired private LocationRepository locationRepo;
    @Autowired private LocationUpdateRepository locationUpdateRepo;
    @Autowired private BusRepository busRepo;
    @Autowired private DriverRepository driverRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private EmergencyReportRepository emergencyReportRepo;
    @Autowired private CrashIncidentRepository crashIncidentRepo;
    @Autowired private BookingRepository bookingRepo;
    @Autowired private SeatReservationRepository seatReservationRepo;

    // --- BUS AVAILABILITY ---

    /** Returns buses whose status is 'Available', as small DTOs (no sensitive fields). */
    @GetMapping("/buses/available")
    public List<BusAvailableDTO> getAvailableBuses() {
        return busRepo.findByStatus("Available").stream()
                .map(b -> new BusAvailableDTO(b.getBusId(), b.getRegistrationNumber(), b.getPassengerCapacity()))
                .collect(Collectors.toList());
    }

    // --- DRIVER LIST ---

    /** Returns active drivers only, joined with their user name, as small DTOs (no password hash). */
    @GetMapping("/drivers")
    public List<DriverSummaryDTO> getDrivers() {
        return driverRepo.findAll().stream()
                .flatMap(d -> userRepo.findById(d.getUserId())
                        // Exclude deactivated users — they cannot be assigned to new trips.
                        .filter(u -> "Active".equalsIgnoreCase(u.getAccountStatus()))
                        .map(u -> new DriverSummaryDTO(d.getUserId().intValue(), u.getFullName(), d.getLicenseNumber(), d.getStatus()))
                        .stream())
                .collect(Collectors.toList());
    }

    // --- TRIP MANAGEMENT ---

    @GetMapping("/trips")
    public List<TransportTripDTO> getAllTrips() {
        return busTripRepo.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/trips/{id}")
    public TransportTripDTO getTripById(@PathVariable int id) {
        return mapToDTO(busTripRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + id)));
    }

    @PostMapping("/trips")
    public TransportTripDTO createTrip(@RequestBody BusTrip trip) {
        Bus bus = validateAndFetchBus(trip);
        // Driver is mandatory when creating a trip; the update path does not re-enforce this.
        if (trip.getDriverUserId() == null) {
            throw new RuntimeException("A driver must be selected");
        }
        validateDriver(trip.getDriverUserId());
        validateLocations(trip.getPickupLocation(), trip.getDropLocation());
        validateTimeRange(trip.getStartTime(), trip.getEta());
        validateNotInPast(trip.getTripDate(), trip.getStartTime());
        checkBusConflict(null, bus.getBusId(), trip.getTripDate(), trip.getStartTime(), trip.getEta(), bus.getRegistrationNumber());
        if (trip.getDriverUserId() != null) {
            checkDriverConflict(null, trip.getDriverUserId(), trip.getTripDate(), trip.getStartTime(), trip.getEta());
        }
        // Replace the shell Bus object (only busId set) with the full entity so FK resolves correctly.
        trip.setBus(bus);
        return mapToDTO(routeService.scheduleTrip(trip));
    }

    @PutMapping("/trips/{id}")
    public TransportTripDTO updateTrip(@PathVariable int id, @RequestBody BusTrip trip) {
        BusTrip existing = busTripRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + id));
        Bus bus = validateAndFetchBus(trip);
        validateDriver(trip.getDriverUserId());
        validateLocations(trip.getPickupLocation(), trip.getDropLocation());
        validateTimeRange(trip.getStartTime(), trip.getEta());
        // Not-in-past is skipped on update: a past trip may still need status or driver corrections.
        checkBusConflict(id, bus.getBusId(), trip.getTripDate(), trip.getStartTime(), trip.getEta(), bus.getRegistrationNumber());
        if (trip.getDriverUserId() != null) {
            checkDriverConflict(id, trip.getDriverUserId(), trip.getTripDate(), trip.getStartTime(), trip.getEta());
        }
        existing.setBus(bus);
        existing.setDriverUserId(trip.getDriverUserId());
        existing.setTripDate(trip.getTripDate());
        existing.setStartTime(trip.getStartTime());
        existing.setEta(trip.getEta());
        existing.setPickupLocation(trip.getPickupLocation());
        existing.setDropLocation(trip.getDropLocation());
        existing.setTripStatus(trip.getTripStatus());
        existing.setOperatingCost(trip.getOperatingCost());
        return mapToDTO(busTripRepo.save(existing));
    }

    @Transactional
    @PutMapping("/trips/{id}/cancel")
    public ResponseEntity<?> cancelTrip(@PathVariable int id, @RequestParam String reason) {
        BusTrip trip = busTripRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + id));

        // Already cancelled — no-op, return 0 bookings cancelled
        if ("Cancelled".equals(trip.getTripStatus())) {
            return ResponseEntity.ok(Map.of("tripId", trip.getTripId(),
                    "tripStatus", "Cancelled", "bookingsCancelled", 0));
        }

        // Capture status before overwriting so we can decide whether bookings need cancelling
        boolean wasCompleted = "Completed".equals(trip.getTripStatus());

        trip.setTripStatus("Cancelled");
        busTripRepo.save(trip);

        TripCancellation cancel = new TripCancellation();
        cancel.setTripId(id);
        cancel.setReason(reason);
        cancel.setCancelledAt(LocalDateTime.now());
        tripCancelRepo.save(cancel);

        // Do NOT cancel bookings for a trip that was already Completed — those trips have already run.
        // Invoices are recalculated from non-cancelled bookings on the next read, so cancelled-trip charges disappear automatically.
        int bookingsCancelled = 0;
        if (!wasCompleted) {
            // Delete seat reservations first (FK: seat_reservation.booking_id → booking.id)
            seatReservationRepo.deleteByTripId(id);
            bookingsCancelled = bookingRepo.cancelBookingsForTrip(id);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("tripId", trip.getTripId());
        resp.put("tripDate", trip.getTripDate());
        resp.put("startTime", trip.getStartTime());
        resp.put("eta", trip.getEta());
        resp.put("tripStatus", trip.getTripStatus());
        resp.put("driverUserId", trip.getDriverUserId());
        resp.put("operatingCost", trip.getOperatingCost());
        resp.put("bookingsCancelled", bookingsCancelled);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/trips/{id}/complete")
    public void completeTrip(@PathVariable int id) {
        BusTrip trip = busTripRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + id));
        trip.setTripStatus("Completed");
        busTripRepo.save(trip);
    }

    @PostMapping("/trips/{id}/location")
    public void recordLocation(@PathVariable int id,
                               @RequestParam double lat,
                               @RequestParam double lng) {
        LocationUpdate update = new LocationUpdate();
        update.setTripId(id);
        update.setLatitude(BigDecimal.valueOf(lat));
        update.setLongitude(BigDecimal.valueOf(lng));
        update.setRecordedAt(LocalDateTime.now());
        locationUpdateRepo.save(update);
    }

    @GetMapping("/trips/{id}/locations")
    public List<LocationUpdate> getTripLocations(@PathVariable int id) {
        return locationUpdateRepo.findByTripIdOrderByRecordedAtAsc(id);
    }

    @GetMapping("/trips/{id}/latest-location")
    public LocationUpdate getLatestLocation(@PathVariable int id) {
        List<LocationUpdate> updates = locationUpdateRepo.findByTripIdOrderByRecordedAtAsc(id);
        return updates.isEmpty() ? null : updates.get(updates.size() - 1);
    }

    // --- PAYMENT MANAGEMENT ---

    @GetMapping("/payments")
    public List<Payment> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @PutMapping("/payments/{id}/cancel")
    public void cancelPayment(@PathVariable int id, @RequestParam String reason) {
        paymentService.cancelPayment(id, reason);
    }

    @GetMapping("/payment-cancellations")
    public List<PaymentCancelation> getAllPaymentCancellations() {
        return paymentCancelRepo.findAll();
    }

    // --- LOCATION CRUD ---

    @GetMapping("/locations")
    public List<Location> getAllLocations() {
        return locationRepo.findAll();
    }

    @PostMapping("/locations")
    public Location addLocation(@RequestBody Location location) {
        return locationRepo.save(location);
    }

    @DeleteMapping("/locations/{id}")
    public void deleteLocation(@PathVariable int id) {
        locationRepo.deleteById(id);
    }

    @PutMapping("/locations/{id}")
    public Location updateLocation(@PathVariable int id, @RequestBody Location location) {
        Location existing = locationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found: " + id));
        existing.setLocationName(location.getLocationName());
        existing.setLatitude(location.getLatitude());
        existing.setLongitude(location.getLongitude());
        return locationRepo.save(existing);
    }

    // ─────────────────────────────────────────────────────────────
    // Emergency Reports (read + status workflow)
    // ─────────────────────────────────────────────────────────────

    /** Returns all emergency reports newest-first; filtered by ?status= if supplied. */
    @GetMapping("/emergency-reports")
    public List<EmergencyReportViewDTO> getEmergencyReports(
            @RequestParam(required = false) String status) {
        List<EmergencyReport> reports = emergencyReportRepo.findAllByOrderByTimestampDesc();
        // Batch-load users and trips to avoid N+1 queries
        Map<Long, User> userMap = batchLoadUsers(
                reports.stream().map(r -> r.getStudentNo() != null ? r.getStudentNo().longValue() : null)
                       .filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Integer, BusTrip> tripMap = batchLoadTrips(
                reports.stream().map(EmergencyReport::getTripId)
                       .filter(Objects::nonNull).collect(Collectors.toSet()));

        return reports.stream()
                .map(r -> {
                    User u = r.getStudentNo() != null ? userMap.get(r.getStudentNo().longValue()) : null;
                    BusTrip trip = r.getTripId() != null ? tripMap.get(r.getTripId()) : null;
                    return new EmergencyReportViewDTO(
                            r.getReportId(),
                            r.getReportTitle(),
                            r.getEmergencyType(),
                            r.getDescription(),
                            u != null ? u.getFullName() : "Unknown",
                            u != null ? u.getRoleName() : null,
                            trip != null ? buildTripLabel(trip) : null,
                            trip != null && trip.getBus() != null ? trip.getBus().getRegistrationNumber() : null,
                            normalizeStatus(r.getResolutionStatus()),
                            r.getTimestamp() != null ? r.getTimestamp().toString() : null
                    );
                })
                .filter(d -> status == null || status.equalsIgnoreCase(d.getStatus()))
                .collect(Collectors.toList());
    }

    /** Advance an emergency report's status (Pending→Acknowledged→Resolved). TRANSPORT_OFFICER only. */
    @PutMapping("/emergency-reports/{id}/status")
    public ResponseEntity<?> updateEmergencyStatus(@PathVariable Integer id,
                                                   @RequestBody StatusUpdateRequest req) {
        String newStatus = req.getStatus();
        if (!"Acknowledged".equals(newStatus) && !"Resolved".equals(newStatus)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status must be Acknowledged or Resolved"));
        }
        EmergencyReport report = emergencyReportRepo.findById(id).orElse(null);
        if (report == null) return ResponseEntity.notFound().build();

        String current = normalizeStatus(report.getResolutionStatus());
        if ("Resolved".equals(current)) {
            return ResponseEntity.status(409).body(Map.of("message", "This report is already resolved"));
        }
        if (statusRank(newStatus) <= statusRank(current)) {
            return ResponseEntity.status(409).body(Map.of("message", "Status cannot move backwards"));
        }
        report.setResolutionStatus(newStatus);
        emergencyReportRepo.save(report);
        return ResponseEntity.ok(Map.of("status", newStatus));
    }

    // ─────────────────────────────────────────────────────────────
    // Crash Incidents (read + status workflow)
    // ─────────────────────────────────────────────────────────────

    /** Returns all crash incidents newest-first; filtered by ?status= if supplied. */
    @GetMapping("/crash-incidents")
    public List<CrashIncidentViewDTO> getCrashIncidents(
            @RequestParam(required = false) String status) {
        List<CrashIncident> incidents = crashIncidentRepo.findAllByOrderByTimestampDesc();
        // Batch-load drivers and trips
        Map<Long, User> driverMap = batchLoadUsers(
                incidents.stream().map(i -> i.getDriverUserId() != null ? i.getDriverUserId().longValue() : null)
                         .filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Integer, BusTrip> tripMap = batchLoadTrips(
                incidents.stream().map(CrashIncident::getTripId)
                         .filter(Objects::nonNull).collect(Collectors.toSet()));

        return incidents.stream()
                .map(i -> {
                    User driver = i.getDriverUserId() != null ? driverMap.get(i.getDriverUserId().longValue()) : null;
                    BusTrip trip = i.getTripId() != null ? tripMap.get(i.getTripId()) : null;
                    // busRegistration: prefer trip's bus, fall back to direct bus_id column
                    String busReg = null;
                    if (trip != null && trip.getBus() != null) {
                        busReg = trip.getBus().getRegistrationNumber();
                    } else if (i.getBusNo() != null) {
                        busReg = busRepo.findById(i.getBusNo()).map(Bus::getRegistrationNumber).orElse(null);
                    }
                    return new CrashIncidentViewDTO(
                            i.getIncidentId(),
                            i.getLocationCoordinates(),
                            i.getSeverityLevel(),
                            i.getDescription(),
                            driver != null ? driver.getFullName() : "Unknown",
                            busReg,
                            trip != null ? buildTripLabel(trip) : null,
                            normalizeStatus(i.getStatus()),
                            i.getTimestamp() != null ? i.getTimestamp().toString() : null
                    );
                })
                .filter(d -> status == null || status.equalsIgnoreCase(d.getStatus()))
                .collect(Collectors.toList());
    }

    /** Advance a crash incident's status (Pending→Acknowledged→Resolved). TRANSPORT_OFFICER only. */
    @PutMapping("/crash-incidents/{id}/status")
    public ResponseEntity<?> updateCrashStatus(@PathVariable Integer id,
                                               @RequestBody StatusUpdateRequest req) {
        String newStatus = req.getStatus();
        if (!"Acknowledged".equals(newStatus) && !"Resolved".equals(newStatus)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status must be Acknowledged or Resolved"));
        }
        CrashIncident incident = crashIncidentRepo.findById(id).orElse(null);
        if (incident == null) return ResponseEntity.notFound().build();

        String current = normalizeStatus(incident.getStatus());
        if ("Resolved".equals(current)) {
            return ResponseEntity.status(409).body(Map.of("message", "This report is already resolved"));
        }
        if (statusRank(newStatus) <= statusRank(current)) {
            return ResponseEntity.status(409).body(Map.of("message", "Status cannot move backwards"));
        }
        incident.setStatus(newStatus);
        crashIncidentRepo.save(incident);
        return ResponseEntity.ok(Map.of("status", newStatus));
    }

    // ─────────────────────────────────────────────────────────────
    // Validation helpers
    // ─────────────────────────────────────────────────────────────

    /** Fetches the bus entity and checks its status is 'Available'. */
    private Bus validateAndFetchBus(BusTrip trip) {
        if (trip.getBus() == null || trip.getBus().getBusId() == null) {
            throw new RuntimeException("A bus must be selected");
        }
        Bus bus = busRepo.findById(trip.getBus().getBusId())
                .orElseThrow(() -> new RuntimeException("Bus not found: " + trip.getBus().getBusId()));
        if (!"Available".equals(bus.getStatus())) {
            throw new RuntimeException(
                "Bus " + bus.getRegistrationNumber() + " is not available for scheduling (status: " + bus.getStatus() + ")");
        }
        return bus;
    }

    /** Checks the driver row exists in the driver table when a driverUserId is supplied. */
    private void validateDriver(Integer driverUserId) {
        if (driverUserId == null) return;
        if (!driverRepo.existsById(driverUserId.longValue())) {
            throw new RuntimeException("Driver not found: " + driverUserId);
        }
    }

    /** Pickup and drop must be set and must be different locations. */
    private void validateLocations(Location pickup, Location drop) {
        if (pickup == null || pickup.getLocationId() == null) {
            throw new RuntimeException("Pickup location is required");
        }
        if (drop == null || drop.getLocationId() == null) {
            throw new RuntimeException("Drop location is required");
        }
        if (pickup.getLocationId().equals(drop.getLocationId())) {
            throw new RuntimeException("Pickup and drop locations must be different");
        }
    }

    /** ETA must be strictly after startTime. */
    private void validateTimeRange(LocalTime start, LocalTime eta) {
        if (start == null) throw new RuntimeException("Start time is required");
        if (eta == null)   throw new RuntimeException("ETA is required");
        if (!eta.isAfter(start)) {
            throw new RuntimeException("ETA must be after the start time");
        }
    }

    /** Trip creation only: the combined date+time must not already have passed. */
    private void validateNotInPast(LocalDate date, LocalTime start) {
        if (date == null) throw new RuntimeException("Trip date is required");
        if (LocalDateTime.of(date, start).isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Trip date and time cannot be in the past");
        }
    }

    /**
     * Two time ranges [s1,e1) and [s2,e2) overlap when s1 < e2 AND s2 < e1.
     * Excludes the trip being updated (excludeTripId) so an edit doesn't conflict with itself.
     * Returns 409 via GlobalExceptionHandler ("already in use" → CONFLICT).
     */
    private void checkBusConflict(Integer excludeTripId, Long busId, LocalDate date,
                                   LocalTime start, LocalTime eta, String busReg) {
        busTripRepo.findByTripDateAndBus_BusIdAndTripStatusNot(date, busId, "Cancelled")
                .stream()
                .filter(t -> !t.getTripId().equals(excludeTripId))
                .filter(t -> t.getStartTime() != null && t.getEta() != null
                          && start.isBefore(t.getEta()) && t.getStartTime().isBefore(eta))
                .findFirst()
                .ifPresent(t -> {
                    throw new RuntimeException(
                        "Bus " + busReg + " is already in use on " + date +
                        " from " + t.getStartTime() + " to " + t.getEta() +
                        " (trip #" + t.getTripId() + ")");
                });
    }

    /** Same overlap logic as checkBusConflict but keyed by driverUserId. */
    private void checkDriverConflict(Integer excludeTripId, Integer driverUserId,
                                      LocalDate date, LocalTime start, LocalTime eta) {
        busTripRepo.findByTripDateAndDriverUserIdAndTripStatusNot(date, driverUserId, "Cancelled")
                .stream()
                .filter(t -> !t.getTripId().equals(excludeTripId))
                .filter(t -> t.getStartTime() != null && t.getEta() != null
                          && start.isBefore(t.getEta()) && t.getStartTime().isBefore(eta))
                .findFirst()
                .ifPresent(t -> {
                    throw new RuntimeException(
                        "Driver (id " + driverUserId + ") is already in use on " + date +
                        " from " + t.getStartTime() + " to " + t.getEta() +
                        " (trip #" + t.getTripId() + ")");
                });
    }

    // ─────────────────────────────────────────────────────────────
    // Incident helpers
    // ─────────────────────────────────────────────────────────────

    /** Maps "Reported" (legacy value) to "Pending" so the UI always shows a consistent status. */
    private String normalizeStatus(String s) {
        return "Reported".equalsIgnoreCase(s) ? "Pending" : (s != null ? s : "Pending");
    }

    /** Pending=0, Acknowledged=1, Resolved=2. Used for forward-only validation. */
    private int statusRank(String s) {
        return switch (normalizeStatus(s)) {
            case "Acknowledged" -> 1;
            case "Resolved"     -> 2;
            default             -> 0; // Pending
        };
    }

    /** "Pickup -> Drop, 10 Oct 07:30" — shown in the TO and admin tables. */
    private String buildTripLabel(BusTrip trip) {
        String pickup  = trip.getPickupLocation() != null ? trip.getPickupLocation().getLocationName() : "?";
        String drop    = trip.getDropLocation()   != null ? trip.getDropLocation().getLocationName()   : "?";
        String dateStr = trip.getTripDate()   != null ? trip.getTripDate().format(DateTimeFormatter.ofPattern("d MMM")) : "?";
        String timeStr = trip.getStartTime()  != null ? trip.getStartTime().toString().substring(0, 5) : "?";
        return pickup + " -> " + drop + ", " + dateStr + " " + timeStr;
    }

    /** One UserRepository call for a set of IDs — avoids N+1 per row. */
    private Map<Long, User> batchLoadUsers(Set<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return userRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));
    }

    /** One BusTripRepository call for a set of trip IDs — avoids N+1 per row. */
    private Map<Integer, BusTrip> batchLoadTrips(Set<Integer> ids) {
        if (ids.isEmpty()) return Map.of();
        return busTripRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(BusTrip::getTripId, t -> t));
    }

    // ─────────────────────────────────────────────────────────────
    // DTO mapping
    // ─────────────────────────────────────────────────────────────

    /** Maps a BusTrip entity to the transport-officer-facing DTO. */
    private TransportTripDTO mapToDTO(BusTrip trip) {
        Bus bus = trip.getBus();
        Long   busId    = bus != null ? bus.getBusId() : null;
        // busLabel example: "NC-5678 (40 seats)"
        String busLabel = (bus != null && bus.getRegistrationNumber() != null)
                ? bus.getRegistrationNumber() + " (" + bus.getPassengerCapacity() + " seats)" : null;

        String driverName = null;
        if (trip.getDriverUserId() != null) {
            // Fetch the driver's name from the Users table; fall back gracefully if the row is missing.
            driverName = userRepo.findById(trip.getDriverUserId().longValue())
                    .map(User::getFullName).orElse("Unknown");
        }

        Location pickup = trip.getPickupLocation();
        Location drop   = trip.getDropLocation();

        return TransportTripDTO.builder()
                .tripId(trip.getTripId())
                .tripDate(trip.getTripDate()   != null ? trip.getTripDate().toString()   : null)
                .startTime(trip.getStartTime() != null ? trip.getStartTime().toString()  : null)
                .eta(trip.getEta()             != null ? trip.getEta().toString()        : null)
                .tripStatus(trip.getTripStatus())
                .operatingCost(trip.getOperatingCost())
                .pickupLocationId(pickup   != null ? pickup.getLocationId()   : null)
                .pickupLocationName(pickup != null ? pickup.getLocationName() : null)
                .dropLocationId(drop       != null ? drop.getLocationId()     : null)
                .dropLocationName(drop     != null ? drop.getLocationName()   : null)
                .busId(busId)
                .busLabel(busLabel)
                .driverUserId(trip.getDriverUserId())
                .driverName(driverName)
                .build();
    }
}
