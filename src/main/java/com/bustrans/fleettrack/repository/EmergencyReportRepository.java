package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.EmergencyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmergencyReportRepository extends JpaRepository<EmergencyReport, Integer> {
    List<EmergencyReport> findByStudentNo(Integer userId);
    List<EmergencyReport> findAllByOrderByTimestampDesc();
}
