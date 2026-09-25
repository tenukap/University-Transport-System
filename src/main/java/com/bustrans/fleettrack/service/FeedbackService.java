package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.dto.FeedbackRequestDTO;
import com.bustrans.fleettrack.dto.FeedbackResponseDTO;
import com.bustrans.fleettrack.entity.Booking;
import com.bustrans.fleettrack.entity.BusTrip;
import com.bustrans.fleettrack.entity.Feedback;
import com.bustrans.fleettrack.repository.BookingRepository;
import com.bustrans.fleettrack.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    // Needed to verify the booking exists, belongs to the user, and its trip date has passed.
    private final BookingRepository bookingRepository;

    /**
     * Records a student's feedback for one of their own bookings, but only once the
     * trip date has passed. Guards ownership, trip-date, rating range and duplicates.
     */
    public FeedbackResponseDTO submitFeedback(Long userId, FeedbackRequestDTO request) {
        if (request.getBookingId() == null) {
            throw new RuntimeException("bookingId is required");
        }

        Integer rating = request.getRating();
        if (rating == null || rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + request.getBookingId()));

        if (booking.getUser() == null || !userId.equals(booking.getUser().getUserId())) {
            throw new RuntimeException("You can only leave feedback for your own bookings");
        }

        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Cancelled bookings cannot be reviewed");
        }

        if (!tripDateHasPassed(booking)) {
            throw new RuntimeException("Feedback can only be left after the trip date has passed");
        }

        if (feedbackRepository.existsByBookingId(booking.getId())) {
            throw new RuntimeException("Feedback has already been submitted for this booking");
        }

        Integer tripId = booking.getBusTrip() != null ? booking.getBusTrip().getTripId() : null;
        String comment = request.getComment() != null ? request.getComment().trim() : "";

        Feedback feedback = Feedback.builder()
                .userId(userId)
                .bookingId(booking.getId())
                .subject("Trip #" + (tripId != null ? tripId : booking.getId()) + " feedback")
                // Message is NOT NULL in the schema; fall back to a placeholder when the
                // student rated without typing a comment.
                .message(comment.isEmpty() ? "(no comment)" : comment)
                .rating(rating)
                .status("Pending")
                .build();

        return mapToDTO(feedbackRepository.save(feedback));
    }

    public List<FeedbackResponseDTO> getFeedbackByUser(Long userId) {
        return feedbackRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .toList();
    }

    public List<FeedbackResponseDTO> getFeedbackByBooking(Long bookingId) {
        return feedbackRepository.findByBookingId(bookingId).stream()
                .map(this::mapToDTO)
                .toList();
    }

    public List<FeedbackResponseDTO> getAllFeedback() {
        return feedbackRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    /** True when the booking's trip has a date that is strictly before today. */
    private boolean tripDateHasPassed(Booking booking) {
        BusTrip trip = booking.getBusTrip();
        return trip != null
                && trip.getTripDate() != null
                && trip.getTripDate().isBefore(LocalDate.now());
    }

    private FeedbackResponseDTO mapToDTO(Feedback feedback) {
        return FeedbackResponseDTO.builder()
                .id(feedback.getFeedbackId())
                .bookingId(feedback.getBookingId())
                .userId(feedback.getUserId())
                .rating(feedback.getRating())
                .comment(feedback.getMessage())
                .status(feedback.getStatus())
                .submittedAt(feedback.getSubmittedAt())
                .build();
    }
}
