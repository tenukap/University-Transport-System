package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripStatusRepository extends JpaRepository<TripStatus, Integer> {
    Optional<TripStatus> findTopByTripIdOrderByUpdatedAtDesc(Integer tripId);
    List<TripStatus> findByTripIdIn(Collection<Integer> tripIds);
}
