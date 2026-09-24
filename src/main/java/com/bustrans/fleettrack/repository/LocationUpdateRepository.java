package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.LocationUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationUpdateRepository extends JpaRepository<LocationUpdate, Integer> {
}
