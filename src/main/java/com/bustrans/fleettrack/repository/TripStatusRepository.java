package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripStatusRepository extends JpaRepository<TripStatus, Integer> {
}
