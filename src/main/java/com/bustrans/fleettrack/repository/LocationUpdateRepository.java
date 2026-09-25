package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.LocationUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationUpdateRepository extends JpaRepository<LocationUpdate, Integer> {
    List<LocationUpdate> findByTripIdOrderByRecordedAtAsc(int tripId);
}

