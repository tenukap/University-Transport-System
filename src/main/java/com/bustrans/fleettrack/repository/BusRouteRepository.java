package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.BusRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusRouteRepository extends JpaRepository<BusRoute, Integer> {
    Optional<BusRoute> findByRouteName(String routeName);
}
