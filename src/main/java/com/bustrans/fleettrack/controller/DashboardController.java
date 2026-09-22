package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.model.Models;
import com.bustrans.fleettrack.service.FleetTrackService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private final FleetTrackService service;

    public DashboardController(FleetTrackService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public Models.DashboardSummary dashboard() {
        return service.summary();
    }

    @GetMapping("/users/{id}/profile")
    public Models.Profile profile(@PathVariable long id) {
        return service.profile(id);
    }

    @PutMapping("/users/{id}/profile")
    public Models.Profile updateProfile(@PathVariable long id, @Valid @RequestBody Models.UpdateProfileRequest request) {
        return service.updateProfile(id, request);
    }

    @GetMapping("/users/{id}/bookings")
    public List<Models.Booking> bookingHistory(@PathVariable long id) {
        return service.bookingHistory(id);
    }

    @GetMapping("/bookings")
    public List<Models.Booking> bookings() {
        return service.listBookings();
    }

    @PostMapping("/bookings")
    public ResponseEntity<Models.Booking> createBooking(@Valid @RequestBody Models.CreateBookingRequest request) {
        return ResponseEntity.status(201).body(service.createBooking(request));
    }

    @DeleteMapping("/bookings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBooking(@PathVariable long id) {
        service.deleteBooking(id);
    }

    @GetMapping("/reports/financial")
    public Models.FinancialReport financialReport(
            @RequestParam(defaultValue = "2000-01-01") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "2100-12-31") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "All Routes") String route,
            @RequestParam(defaultValue = "All Groups") String studentGroup) {
        return service.financialReport(startDate, endDate, route, studentGroup);
    }

    @GetMapping(value = "/reports/financial.csv", produces = "text/csv")
    public ResponseEntity<String> financialCsv(
            @RequestParam(defaultValue = "2000-01-01") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "2100-12-31") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "All Routes") String route,
            @RequestParam(defaultValue = "All Groups") String studentGroup) {
        Models.FinancialReport report = service.financialReport(startDate, endDate, route, studentGroup);
        StringBuilder csv = new StringBuilder("id,route,passenger,travelDate,departureTime,status,fare\n");
        report.bookings().forEach(booking -> csv.append(booking.id()).append(',')
                .append(booking.route()).append(',').append(booking.passenger()).append(',')
                .append(booking.travelDate()).append(',').append(booking.departureTime()).append(',')
                .append(booking.status()).append(',').append(booking.fare()).append('\n'));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=financial-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    @GetMapping("/feedback")
    public List<Models.Feedback> feedback() {
        return service.listFeedback();
    }

    @PostMapping("/announcements")
    public ResponseEntity<Models.Announcement> createAnnouncement(@Valid @RequestBody Models.CreateAnnouncementRequest request) {
        return ResponseEntity.status(201).body(service.createAnnouncement(request));
    }

    @DeleteMapping("/announcements/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAnnouncement(@PathVariable long id) {
        service.deleteAnnouncement(id);
    }
}
