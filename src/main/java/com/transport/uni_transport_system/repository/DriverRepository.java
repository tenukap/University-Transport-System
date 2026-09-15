package com.transport.uni_transport_system.repository;
import com.transport.uni_transport_system.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, Long> {
}