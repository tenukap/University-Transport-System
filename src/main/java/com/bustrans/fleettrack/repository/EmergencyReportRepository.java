package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.EmergencyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmergencyReportRepository extends JpaRepository<EmergencyReport, Integer> {
}
