package com.transport.uni_transport_system.repository;

import com.transport.uni_transport_system.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Integer> {
}
