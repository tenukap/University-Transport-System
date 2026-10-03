package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.CrashIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrashIncidentRepository extends JpaRepository<CrashIncident, Integer> {
    List<CrashIncident> findByDriverUserId(Integer driverUserId);
    List<CrashIncident> findAllByOrderByTimestampDesc();
}
