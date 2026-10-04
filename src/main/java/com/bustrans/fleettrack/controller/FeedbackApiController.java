package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.dto.*;
import com.bustrans.fleettrack.entity.Booking;
import com.bustrans.fleettrack.entity.Feedback;
import com.bustrans.fleettrack.entity.User;
import com.bustrans.fleettrack.repository.BookingRepository;
import com.bustrans.fleettrack.repository.FeedbackRepository;
import com.bustrans.fleettrack.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackApiController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final BookingRepository bookingRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    public FeedbackApiController(BookingRepository bookingRepository,
                                 FeedbackRepository feedbackRepository,
                                 UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
    }

    // --- GET /api/feedback/eligible (STUDENT) ---
    @GetMapping("/eligible")
    public List<EligibleTripDTO> eligible(Authentication auth) {
        long userId = userId(auth);
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // Query returns trips with tripDate <= today; we then filter same-day trips by startTime in Java
        // to avoid the SQL Server TIME vs. datetime type-incompatibility error.
        List<Booking> candidates = bookingRepository.findDepartedConfirmedBookings(userId, today);

        return candidates.stream()
                .filter(b -> {
                    LocalDate tripDate = b.getBusTrip().getTripDate();
                    LocalTime tripStart = b.getBusTrip().getStartTime();
                    return tripDate.isBefore(today)
                            || (tripDate.isEqual(today) && !tripStart.isAfter(now));
                })
                .filter(b -> !feedbackRepository.existsByBookingId(b.getId()))
                .map(b -> new EligibleTripDTO(b.getId(), tripLabel(b), busReg(b)))
                .collect(Collectors.toList());
    }

    // --- POST /api/feedback (STUDENT) ---
    @PostMapping
    public ResponseEntity<?> submit(@RequestBody FeedbackSubmitRequest req, Authentication auth) {
        long userId = userId(auth);

        if (req.getBookingId() == null) {
            return error(HttpStatus.BAD_REQUEST, "bookingId is required");
        }

        // Booking must exist, belong to this student, and be CONFIRMED
        Optional<Booking> bookingOpt = bookingRepository.findById(req.getBookingId());
        if (bookingOpt.isEmpty()
                || !bookingOpt.get().getUser().getUserId().equals(userId)
                || !"CONFIRMED".equals(bookingOpt.get().getStatus())) {
            return error(HttpStatus.NOT_FOUND, "Booking not found");
        }
        Booking booking = bookingOpt.get();

        // Trip must have departed
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalDate tripDate = booking.getBusTrip().getTripDate();
        LocalTime tripStart = booking.getBusTrip().getStartTime();
        boolean departed = tripDate.isBefore(today)
                || (tripDate.isEqual(today) && !tripStart.isAfter(now));
        if (!departed) {
            return error(HttpStatus.BAD_REQUEST, "You can give feedback after the trip has departed");
        }

        // Rating: required, 1–5
        if (req.getRating() == null) {
            return error(HttpStatus.BAD_REQUEST, "Rating is required");
        }
        if (req.getRating() < 1 || req.getRating() > 5) {
            return error(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }

        // Comments: required, 1–1000 chars after trim
        if (req.getComments() == null || req.getComments().trim().isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "Comment is required");
        }
        String trimmed = req.getComments().trim();
        if (trimmed.length() > 1000) {
            return error(HttpStatus.BAD_REQUEST, "Comment cannot exceed 1000 characters");
        }

        // Duplicate: one feedback per booking
        if (feedbackRepository.existsByBookingId(req.getBookingId())) {
            return error(HttpStatus.CONFLICT, "You have already given feedback for this trip");
        }

        User user = userRepository.findById(userId).orElseThrow();

        Feedback fb = new Feedback();
        fb.setUser(user);
        fb.setBookingId(req.getBookingId());
        fb.setRating(req.getRating());
        fb.setComments(trimmed);
        fb.setSubject(tripSubject(booking));
        fb.setStatus("Pending");
        fb.setSubmittedAt(LocalDateTime.now());

        Feedback saved = feedbackRepository.save(fb);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", saved.getFeedbackId()));
    }

    // --- GET /api/feedback/mine (STUDENT) ---
    @GetMapping("/mine")
    public List<FeedbackMineDTO> mine(Authentication auth) {
        long userId = userId(auth);
        List<Feedback> list = feedbackRepository.findByUser_UserIdOrderBySubmittedAtDesc(userId);

        return list.stream().map(fb -> {
            String label = fb.getBookingId() != null
                    ? bookingRepository.findById(fb.getBookingId())
                            .map(this::tripLabel).orElse("—")
                    : fb.getSubject();
            return new FeedbackMineDTO(fb.getFeedbackId(), label, fb.getRating(),
                    fb.getComments(), fb.getStatus(), fb.getSubmittedAt(),
                    fb.getAdminResponse(), fb.getReviewedAt());
        }).collect(Collectors.toList());
    }

    // --- GET /api/feedback (ADMIN) ---
    @GetMapping
    public List<FeedbackAdminDTO> all(@RequestParam(required = false) String status) {
        List<Feedback> list = (status != null && !status.isBlank())
                ? feedbackRepository.findByStatusOrderBySubmittedAtDesc(status)
                : feedbackRepository.findAllByOrderBySubmittedAtDesc();

        return list.stream().map(fb -> {
            String tripLabel = fb.getBookingId() != null
                    ? bookingRepository.findById(fb.getBookingId()).map(this::tripLabel).orElse("—")
                    : fb.getSubject();
            String busReg = fb.getBookingId() != null
                    ? bookingRepository.findById(fb.getBookingId()).map(this::busReg).orElse("—")
                    : "—";
            String reviewerName = fb.getReviewedBy() != null
                    ? fb.getReviewedBy().getFullName() : null;
            return new FeedbackAdminDTO(
                    fb.getFeedbackId(),
                    fb.getUser().getFullName(),
                    String.valueOf(fb.getUser().getUserId()),
                    tripLabel, busReg, fb.getRating(), fb.getComments(),
                    fb.getStatus(), fb.getSubmittedAt(),
                    fb.getAdminResponse(), fb.getReviewedAt(), reviewerName);
        }).collect(Collectors.toList());
    }

    // --- GET /api/feedback/summary (ADMIN) ---
    @GetMapping("/summary")
    public FeedbackSummaryDTO summary() {
        List<Feedback> all = feedbackRepository.findAll();
        long total = all.size();
        long pending = all.stream().filter(f -> "Pending".equals(f.getStatus())).count();
        long reviewed = all.stream().filter(f -> "Reviewed".equals(f.getStatus())).count();
        OptionalDouble avg = all.stream()
                .filter(f -> f.getRating() != null)
                .mapToInt(Feedback::getRating)
                .average();
        Double averageRating = avg.isPresent()
                ? Math.round(avg.getAsDouble() * 10.0) / 10.0
                : null;
        return new FeedbackSummaryDTO(total, pending, reviewed, averageRating);
    }

    // --- PUT /api/feedback/{id}/review (ADMIN) ---
    @PutMapping("/{id}/review")
    public ResponseEntity<?> review(@PathVariable Integer id,
                                    @RequestBody(required = false) ReviewRequest req,
                                    Authentication auth) {
        Feedback fb = feedbackRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Feedback not found"));

        String response = req != null ? req.getAdminResponse() : null;
        if (response != null && response.trim().length() > 1000) {
            return error(HttpStatus.BAD_REQUEST, "Reply cannot exceed 1000 characters");
        }

        fb.setStatus("Reviewed");
        fb.setReviewedAt(LocalDateTime.now());
        fb.setAdminResponse(response != null ? response.trim() : null);
        fb.setReviewedById(Math.toIntExact(userId(auth)));
        feedbackRepository.save(fb);

        return ResponseEntity.ok(Map.of("status", "Reviewed"));
    }

    // ---- helpers ----

    private long userId(Authentication auth) {
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid authentication token");
        }
    }

    private String tripLabel(Booking b) {
        String pickup = b.getBusTrip().getPickupLocation() != null
                ? b.getBusTrip().getPickupLocation().getLocationName() : "?";
        String drop = b.getBusTrip().getDropLocation() != null
                ? b.getBusTrip().getDropLocation().getLocationName() : "?";
        String date = b.getBusTrip().getTripDate() != null
                ? b.getBusTrip().getTripDate().format(DATE_FMT) : "?";
        String time = b.getBusTrip().getStartTime() != null
                ? b.getBusTrip().getStartTime().format(TIME_FMT) : "?";
        return pickup + " -> " + drop + ", " + date + " " + time;
    }

    private String tripSubject(Booking b) {
        String pickup = b.getBusTrip().getPickupLocation() != null
                ? b.getBusTrip().getPickupLocation().getLocationName() : "?";
        String drop = b.getBusTrip().getDropLocation() != null
                ? b.getBusTrip().getDropLocation().getLocationName() : "?";
        String date = b.getBusTrip().getTripDate() != null
                ? b.getBusTrip().getTripDate().format(DATE_FMT) : "?";
        String raw = "Trip feedback: " + pickup + " -> " + drop + ", " + date;
        return raw.length() <= 150 ? raw : raw.substring(0, 150);
    }

    private String busReg(Booking b) {
        return b.getBusTrip().getBus() != null
                ? b.getBusTrip().getBus().getRegistrationNumber() : "—";
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
