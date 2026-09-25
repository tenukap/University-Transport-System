package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Integer> {
}
