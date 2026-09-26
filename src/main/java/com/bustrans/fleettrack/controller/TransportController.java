package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.entity.BusRoute;
import com.bustrans.fleettrack.entity.BusTrip;
import com.bustrans.fleettrack.entity.Payment;
import com.bustrans.fleettrack.repository.PaymentRepository;
import com.bustrans.fleettrack.service.PaymentService;
import com.bustrans.fleettrack.service.RouteService;
import com.bustrans.fleettrack.service.TripService;
import com.bustrans.fleettrack.entity.Location;
import com.bustrans.fleettrack.service.LocationService;
import com.bustrans.fleettrack.entity.PaymentCancellation;
import com.bustrans.fleettrack.repository.PaymentCancellationRepository;
import com.bustrans.fleettrack.entity.LocationUpdate;
import com.bustrans.fleettrack.repository.LocationUpdateRepository;
import com.bustrans.fleettrack.repository.BusTripRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allows your frontend to connect
public class TransportController {

    @Autowired private RouteService routeService;
    @Autowired private TripService tripService;
    @Autowired private PaymentService paymentService;
    @Autowired private PaymentRepository paymentRepo;
    @Autowired private LocationService locationService;
    @Autowired private PaymentCancellationRepository paymentCancelRepo;
    @Autowired private LocationUpdateRepository locationUpdateRepo;
    @Autowired private BusTripRepository busTripRepo;


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
        return tripService.getAllTrips();
    }

    // GET a single trip by ID
    @GetMapping("/trips/{id}")
    public BusTrip getTripById(@PathVariable int id) {
        return tripService.getTripById(id);
    }

    // CREATE a new trip
    @PostMapping("/trips")
    public BusTrip createTrip(@RequestBody BusTrip trip) {
        return routeService.scheduleTrip(trip);
    }

    // CANCEL a trip
    @PutMapping("/trips/{id}/cancel")
    public BusTrip cancelTrip(@PathVariable int id, @RequestParam String reason) {
        return tripService.cancelTrip(id, reason);
    }


    // --- DRIVER PROGRESS ---
    @PostMapping("/trips/{id}/location")
    public void updateLocation(@PathVariable int id, @RequestParam double lat, @RequestParam double lng) {
        tripService.updateLocation(id, lat, lng);
    }

    @PutMapping("/trips/{id}/complete")
    public void completeTrip(@PathVariable int id) {
        tripService.completeTrip(id);
    }

    // Get all location updates for a specific trip
    @GetMapping("/trips/{id}/locations")
    public List<LocationUpdate> getTripLocations(@PathVariable int id) {
        return locationUpdateRepo.findByTripIdOrderByRecordedAtAsc(id);
    }

    // Get the latest location for a specific trip
    @GetMapping("/trips/{id}/latest-location")
    public LocationUpdate getLatestLocation(@PathVariable int id) {
        List<LocationUpdate> updates = locationUpdateRepo.findByTripIdOrderByRecordedAtAsc(id);
        if (updates.isEmpty()) return null;
        return updates.get(updates.size() - 1);
    }

    // --- PAYMENT CANCELLATION ---
    @PutMapping("/payments/{id}/cancel")
    public void cancelPayment(@PathVariable int id, @RequestParam String reason) {
        paymentService.cancelPayment(id, reason);
    }

    @GetMapping("/payments")
    public List<Payment> getAllPayments() {
        return paymentService.getAllPayments();  // ← changed from paymentRepo.findAll()
    }

    @GetMapping("/payment-cancellations")
    public List<PaymentCancellation> getAllPaymentCancellations() {
        return paymentCancelRepo.findAll();
    }


    // LOCATIONS - CRUD
    @GetMapping("/locations")
    public List<Location> getAllLocations() {
        return locationService.getAllLocations();
    }

    @PostMapping("/locations")
    public Location addLocation(@RequestBody Location location) {
        return locationService.addLocation(location);
    }

    @DeleteMapping("/locations/{id}")
    public void deleteLocation(@PathVariable int id) {
        locationService.deleteLocation(id);
    }


    // ==========================================
    // UPDATE ENDPOINTS (U in CRUD)

    // UPDATE a Trip
    @PutMapping("/trips/{id}")
    public BusTrip updateTrip(@PathVariable int id, @RequestBody BusTrip trip) {
        return tripService.updateTrip(id, trip);
    }

    // UPDATE a Location
    @PutMapping("/locations/{id}")
    public Location updateLocation(@PathVariable int id, @RequestBody Location location) {
        return locationService.updateLocation(id, location);
    }
}
