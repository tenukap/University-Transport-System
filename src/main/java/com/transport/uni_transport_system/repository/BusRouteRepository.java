package com.transport.uni_transport_system.repository;

import com.transport.uni_transport_system.entity.BusRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BusRouteRepository extends JpaRepository<BusRoute, Integer> {
    // For the "Check Duplicate Route" logic in your Sequence Diagram
    Optional<BusRoute> findByRouteName(String routeName);
}
