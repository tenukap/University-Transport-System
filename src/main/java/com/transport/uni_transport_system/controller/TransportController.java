package com.transport.uni_transport_system.controller;

import com.transport.uni_transport_system.entity.BusRoute;
import com.transport.uni_transport_system.entity.BusTrip;
import com.transport.uni_transport_system.service.PaymentService;
import com.transport.uni_transport_system.service.RouteService;
import com.transport.uni_transport_system.service.TripService;
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
    @PostMapping("/trips")
    public BusTrip createTrip(@RequestBody BusTrip trip) {
        return routeService.scheduleTrip(trip);
    }

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

    // --- PAYMENT CANCELLATION ---
    @PutMapping("/payments/{id}/cancel")
    public void cancelPayment(@PathVariable int id, @RequestParam String reason) {
        paymentService.cancelPayment(id, reason);
    }
}
