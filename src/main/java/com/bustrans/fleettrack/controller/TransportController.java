package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.dto.BusAvailableDTO;
import com.bustrans.fleettrack.dto.DriverSummaryDTO;
import com.bustrans.fleettrack.dto.TransportTripDTO;
import com.bustrans.fleettrack.entity.*;
import com.bustrans.fleettrack.repository.*;
import com.bustrans.fleettrack.service.PaymentService;
import com.bustrans.fleettrack.service.RouteService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
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

    // --- ROUTE CRUD ---

    @PostMapping("/routes")
    public BusRoute createRoute(@RequestBody BusRoute route) {
        return routeService.addRoute(route);
    }

    @GetMapping("/routes")
    public List<BusRoute> getRoutes() {
        return routeService.getAllRoutes();
    }

    @PutMapping("/routes/{id}")
    public BusRoute updateRoute(@PathVariable int id, @RequestBody BusRoute route) {
        return routeService.updateRoute(id, route);
    }

    @DeleteMapping("/routes/{id}")
    public void deleteRoute(@PathVariable int id) {
        routeService.deleteRoute(id);
    }

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

    @PutMapping("/trips/{id}/cancel")
    public BusTrip cancelTrip(@PathVariable int id, @RequestParam String reason) {
        BusTrip trip = busTripRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + id));
        trip.setTripStatus("Cancelled");
        busTripRepo.save(trip);
        TripCancellation cancel = new TripCancellation();
        cancel.setTripId(id);
        cancel.setReason(reason);
        cancel.setCancelledAt(LocalDateTime.now());
        tripCancelRepo.save(cancel);
        return trip;
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
