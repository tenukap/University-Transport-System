package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.CrashIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CrashIncidentRepository extends JpaRepository<CrashIncident, Integer> {
}
