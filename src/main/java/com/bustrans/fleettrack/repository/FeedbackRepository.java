package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}