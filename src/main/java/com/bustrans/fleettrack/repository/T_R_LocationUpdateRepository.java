package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.T_R_LocationUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface T_R_LocationUpdateRepository extends JpaRepository<T_R_LocationUpdate, Integer> {
    List<T_R_LocationUpdate> findByTripIdOrderByRecordedAtAsc(int tripId);
}

