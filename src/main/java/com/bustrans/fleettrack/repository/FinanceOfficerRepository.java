package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.FinanceOfficer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceOfficerRepository extends JpaRepository<FinanceOfficer, Long> {
    boolean existsByEmployeeId(String employeeId);
}
