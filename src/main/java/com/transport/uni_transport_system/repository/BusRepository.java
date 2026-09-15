package com.transport.uni_transport_system.repository;
import com.transport.uni_transport_system.entity.Bus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusRepository extends JpaRepository<Bus, Long> {
}