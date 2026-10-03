package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.entity.Bus;
import com.bustrans.fleettrack.service.BusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/buses")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class BusController {

    @Autowired
    private BusService busService;

    /** Add a new bus. Validates registration (unique, max 50) and capacity (1–100). */
    @PostMapping("/add")
    public Bus addBus(@RequestBody Bus bus) {
        return busService.addBus(bus.getRegistrationNumber(), bus.getPassengerCapacity());
    }

    @GetMapping("/all")
    public List<Bus> getAllBuses() {
        return busService.getAllBuses();
    }

    @GetMapping("/{id}")
    public Bus getBusById(@PathVariable Long id) {
        return busService.getBusById(id);
    }

    /** Edit registration and/or capacity. Capacity guard prevents reducing below active booking count. */
    @PutMapping("/{id}")
    public Bus updateBus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String reg = body.get("registrationNumber") != null ? body.get("registrationNumber").toString() : null;
        Integer cap = body.get("passengerCapacity") != null
                ? ((Number) body.get("passengerCapacity")).intValue() : null;
        return busService.updateBus(id, reg, cap);
    }

    /** Change bus status. Moving away from Available while upcoming trips exist → 409. */
    @PatchMapping("/{id}/status")
    public Bus setStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "'status' field is required");
        }
        return busService.setStatus(id, status);
    }

    /** Hard-delete. Blocked if the bus has any trip history. */
    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBus(@PathVariable Long id) {
        busService.deleteBus(id);
    }
}
