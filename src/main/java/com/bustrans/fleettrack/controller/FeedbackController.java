package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.dto.FeedbackRequestDTO;
import com.bustrans.fleettrack.dto.FeedbackResponseDTO;
import com.bustrans.fleettrack.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<FeedbackResponseDTO> submitFeedback(@RequestBody FeedbackRequestDTO request) {
        FeedbackResponseDTO response = feedbackService.submitFeedback(currentUserId(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/user/{userId}")
    public List<FeedbackResponseDTO> getFeedbackByUser(@PathVariable Long userId) {
        return feedbackService.getFeedbackByUser(userId);
    }

    @GetMapping("/booking/{bookingId}")
    public List<FeedbackResponseDTO> getFeedbackByBooking(@PathVariable Long bookingId) {
        return feedbackService.getFeedbackByBooking(bookingId);
    }

    @GetMapping
    public List<FeedbackResponseDTO> getAllFeedback() {
        return feedbackService.getAllFeedback();
    }

    /** The authenticated user's id, taken from the JWT subject (principal). */
    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        try {
            return Long.parseLong(auth.getPrincipal().toString());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user token principal");
        }
    }
}
