package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.entity.Bus;
import com.bustrans.fleettrack.service.BusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/buses")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class BusController {

    @Autowired
    private BusService busService;

    @PostMapping("/add")
    public Bus addBus(@RequestBody Bus bus) {
        return busService.saveBus(bus);
    }

    @GetMapping("/all")
    public List<Bus> getAllBuses() {
        return busService.getAllBuses();
    }

    @GetMapping("/{id}")
    public Bus getBusById(@PathVariable Long id) {
        return busService.getBusById(id);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteBus(@PathVariable Long id) {
        busService.deleteBus(id);
    }
}