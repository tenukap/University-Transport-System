package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.entity.T_R_BusRoute;
import com.bustrans.fleettrack.entity.T_R_BusTrip;
import com.bustrans.fleettrack.entity.T_R_Payment;
import com.bustrans.fleettrack.repository.T_R_PaymentRepository;
import com.bustrans.fleettrack.service.T_R_PaymentService;
import com.bustrans.fleettrack.service.T_R_RouteService;
import com.bustrans.fleettrack.service.T_R_TripService;
import com.bustrans.fleettrack.entity.T_R_Location;
import com.bustrans.fleettrack.service.T_R_LocationService;
import com.bustrans.fleettrack.entity.T_R_PaymentCancellation;
import com.bustrans.fleettrack.repository.T_R_PaymentCancellationRepository;
import com.bustrans.fleettrack.entity.T_R_LocationUpdate;
import com.bustrans.fleettrack.repository.T_R_LocationUpdateRepository;
import com.bustrans.fleettrack.repository.T_R_BusTripRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allows your frontend to connect
public class T_R_TransportController {

    @Autowired private T_R_RouteService routeService;
    @Autowired private T_R_TripService tripService;
    @Autowired private T_R_PaymentService paymentService;
    @Autowired private T_R_PaymentRepository paymentRepo;
    @Autowired private T_R_LocationService locationService;
    @Autowired private T_R_PaymentCancellationRepository paymentCancelRepo;
    @Autowired private T_R_LocationUpdateRepository locationUpdateRepo;
    @Autowired private T_R_BusTripRepository busTripRepo;


    // --- ROUTE CRUD ---
    @PostMapping("/routes")
    public T_R_BusRoute createRoute(@RequestBody T_R_BusRoute route) {
        return routeService.addRoute(route);
    }

    @GetMapping("/routes")
    public List<T_R_BusRoute> getRoutes() {
        return routeService.getAllRoutes();
    }

    @PutMapping("/routes/{id}")
    public T_R_BusRoute updateRoute(@PathVariable int id, @RequestBody T_R_BusRoute route) {
        return routeService.updateRoute(id, route);
    }

    @DeleteMapping("/routes/{id}")
    public void deleteRoute(@PathVariable int id) {
        routeService.deleteRoute(id);
    }

    // --- TRIP MANAGEMENT ---
    @GetMapping("/trips")
    public List<T_R_BusTrip> getAllTrips() {
        return tripService.getAllTrips();
    }

    // GET a single trip by ID
    @GetMapping("/trips/{id}")
    public T_R_BusTrip getTripById(@PathVariable int id) {
        return tripService.getTripById(id);
    }

    // CREATE a new trip
    @PostMapping("/trips")
    public T_R_BusTrip createTrip(@RequestBody T_R_BusTrip trip) {
        return routeService.scheduleTrip(trip);
    }

    // CANCEL a trip
    @PutMapping("/trips/{id}/cancel")
    public T_R_BusTrip cancelTrip(@PathVariable int id, @RequestParam String reason) {
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
    public List<T_R_LocationUpdate> getTripLocations(@PathVariable int id) {
        return locationUpdateRepo.findByTripIdOrderByRecordedAtAsc(id);
    }

    // Get the latest location for a specific trip
    @GetMapping("/trips/{id}/latest-location")
    public T_R_LocationUpdate getLatestLocation(@PathVariable int id) {
        List<T_R_LocationUpdate> updates = locationUpdateRepo.findByTripIdOrderByRecordedAtAsc(id);
        if (updates.isEmpty()) return null;
        return updates.get(updates.size() - 1);
    }

    // --- PAYMENT CANCELLATION ---
    @PutMapping("/payments/{id}/cancel")
    public void cancelPayment(@PathVariable int id, @RequestParam String reason) {
        paymentService.cancelPayment(id, reason);
    }

    @GetMapping("/payments")
    public List<T_R_Payment> getAllPayments() {
        return paymentService.getAllPayments();  // ← changed from paymentRepo.findAll()
    }

    @GetMapping("/payment-cancellations")
    public List<T_R_PaymentCancellation> getAllPaymentCancellations() {
        return paymentCancelRepo.findAll();
    }


    // LOCATIONS - CRUD
    @GetMapping("/locations")
    public List<T_R_Location> getAllLocations() {
        return locationService.getAllLocations();
    }

    @PostMapping("/locations")
    public T_R_Location addLocation(@RequestBody T_R_Location location) {
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
    public T_R_BusTrip updateTrip(@PathVariable int id, @RequestBody T_R_BusTrip trip) {
        return tripService.updateTrip(id, trip);
    }

    // UPDATE a Location
    @PutMapping("/locations/{id}")
    public T_R_Location updateLocation(@PathVariable int id, @RequestBody T_R_Location location) {
        return locationService.updateLocation(id, location);
    }
}
