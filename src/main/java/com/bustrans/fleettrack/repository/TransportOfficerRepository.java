package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.TransportOfficer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransportOfficerRepository extends JpaRepository<TransportOfficer, Long> {
    boolean existsByEmployeeId(String employeeId);
}
