package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.TripCancellation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripCancellationRepository extends JpaRepository<TripCancellation, Integer> {
}
