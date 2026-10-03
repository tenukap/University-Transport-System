package com.bustrans.fleettrack.repository;
import com.bustrans.fleettrack.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    // Used by UserService to reject a duplicate licence before the user row is saved.
    boolean existsByLicenseNumber(String licenseNumber);
}