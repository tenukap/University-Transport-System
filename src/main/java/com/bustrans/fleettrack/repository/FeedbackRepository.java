package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {

    List<Feedback> findByUserId(Long userId);

    List<Feedback> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);
}
