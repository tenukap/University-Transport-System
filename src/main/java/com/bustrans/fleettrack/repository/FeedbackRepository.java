package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    List<Feedback> findByUser_UserId(Long userId);
    Optional<Feedback> findByFeedbackIdAndUser_UserId(Integer feedbackId, Long userId);
}
