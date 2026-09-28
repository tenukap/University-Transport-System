package com.bustrans.fleettrack.repository;
import com.bustrans.fleettrack.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, Long> {
}