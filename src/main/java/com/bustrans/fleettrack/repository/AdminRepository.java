package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    // Used by UserService to reject a duplicate employee_id before the user row is saved.
    boolean existsByEmployeeId(String employeeId);
}
