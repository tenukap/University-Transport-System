package com.transport.uni_transport_system.controller;

import com.transport.uni_transport_system.dto.TripResponseDTO;
import com.transport.uni_transport_system.entity.BusTrip;
import com.transport.uni_transport_system.repository.BusTripRepository;
import com.transport.uni_transport_system.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/trips")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final BusTripRepository busTripRepository;

    @GetMapping
    public ResponseEntity<?> getTrips(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<TripResponseDTO> trips = (date != null)
                    ? tripService.getTripsByDate(date)
                    : tripService.getAllTrips();
            return new ResponseEntity<>(trips, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTripById(@PathVariable Integer id) {
        Optional<BusTrip> trip = busTripRepository.findById(id);
        return trip
                .map(t -> new ResponseEntity<>(tripService.mapToDTO(t), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
