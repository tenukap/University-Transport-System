package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.BusTrip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BusTripRepository extends JpaRepository<BusTrip, Integer> {

    List<BusTrip> findByTripDate(LocalDate tripDate);

    List<BusTrip> findByPickupLocationId(Integer pickupLocationId);
}
