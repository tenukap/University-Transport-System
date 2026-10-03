package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BusRepository extends JpaRepository<Bus, Long> {
    List<Bus> findByStatus(String status);
    // Used to reject duplicate registrations at add time.
    boolean existsByRegistrationNumber(String registrationNumber);
    // Used to reject duplicate registrations at edit time (allows the bus to keep its own number).
    boolean existsByRegistrationNumberAndBusIdNot(String registrationNumber, Long busId);
}
