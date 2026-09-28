package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.entity.Driver;
import com.bustrans.fleettrack.service.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class DriverController {

    @Autowired
    private DriverService driverService;

    @PostMapping("/add")
    public Driver addDriver(@RequestBody Driver driver) {
        return driverService.saveDriver(driver);
    }

    @GetMapping("/all")
    public List<Driver> getAllDrivers() {
        return driverService.getAllDrivers();
    }

    @GetMapping("/{id}")
    public Driver getDriverById(@PathVariable Long id) {
        return driverService.getDriverById(id);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteDriver(@PathVariable Long id) {
        driverService.deleteDriver(id);
    }
}