package com.transport.uni_transport_system.repository;

import com.transport.uni_transport_system.entity.TripCancellation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripCancellationRepository extends JpaRepository<TripCancellation, Integer> {
}
