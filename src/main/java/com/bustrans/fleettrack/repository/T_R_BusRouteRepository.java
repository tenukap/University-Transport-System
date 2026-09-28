package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.T_R_BusRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface T_R_BusRouteRepository extends JpaRepository<T_R_BusRoute, Integer> {
    // For the "Check Duplicate Route" logic in your Sequence Diagram
    Optional<T_R_BusRoute> findByRouteName(String routeName);
}
