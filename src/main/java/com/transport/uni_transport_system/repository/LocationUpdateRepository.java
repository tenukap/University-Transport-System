package com.transport.uni_transport_system.repository;

import com.transport.uni_transport_system.entity.LocationUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationUpdateRepository extends JpaRepository<LocationUpdate, Integer> {
}
