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
public interface BusTripRepository extends JpaRepository<BusTrip, Integer> {
    List<BusTrip> findByTripDate(LocalDate tripDate);
    List<BusTrip> findByPickupLocation_LocationId(Integer locationId);
    List<BusTrip> findByDropLocation_LocationId(Integer locationId);
    List<BusTrip> findByTripStatus(String tripStatus);
}
