package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    List<Feedback> findByUser_UserId(Long userId);
    Optional<Feedback> findByFeedbackIdAndUser_UserId(Integer feedbackId, Long userId);

    /** Detaches feedback from a deleted booking; keeps the feedback itself intact. */
    @Modifying
    @Query("UPDATE Feedback f SET f.bookingId = NULL WHERE f.bookingId = :bookingId")
    void clearBookingId(@Param("bookingId") Long bookingId);
}
