package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.entity.*;
import com.bustrans.fleettrack.repository.*;
import com.bustrans.fleettrack.service.PaymentService;
import com.bustrans.fleettrack.service.RouteService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    // --- TRIP MANAGEMENT ---

    @GetMapping("/trips")
    public List<BusTrip> getAllTrips() {
        return busTripRepo.findAll();
    }

    @GetMapping("/trips/{id}")
    public BusTrip getTripById(@PathVariable int id) {
        return busTripRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + id));
    }

    @PostMapping("/trips")
    public BusTrip createTrip(@RequestBody BusTrip trip) {
        return routeService.scheduleTrip(trip);
    }

    @PutMapping("/trips/{id}")
    public BusTrip updateTrip(@PathVariable int id, @RequestBody BusTrip trip) {
        BusTrip existing = busTripRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + id));
        existing.setTripDate(trip.getTripDate());
        existing.setStartTime(trip.getStartTime());
        existing.setEta(trip.getEta());
        existing.setPickupLocation(trip.getPickupLocation());
        existing.setDropLocation(trip.getDropLocation());
        existing.setTripStatus(trip.getTripStatus());
        existing.setOperatingCost(trip.getOperatingCost());
        return busTripRepo.save(existing);
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
}
