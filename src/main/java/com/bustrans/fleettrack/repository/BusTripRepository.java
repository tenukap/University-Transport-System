package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.BusTrip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusTripRepository extends JpaRepository<BusTrip, Integer> {
}
