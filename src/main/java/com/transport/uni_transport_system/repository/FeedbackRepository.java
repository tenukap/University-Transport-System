package com.transport.uni_transport_system.repository;

import com.transport.uni_transport_system.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}