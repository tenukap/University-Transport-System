package com.transport.uni_transport_system.controller;

import com.transport.uni_transport_system.entity.Bus;
import com.transport.uni_transport_system.service.BusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/buses")
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