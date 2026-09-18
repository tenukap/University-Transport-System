package com.transport.uni_transport_system.repository;

import com.transport.uni_transport_system.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByStudentId(Long studentId);

    List<Booking> findByStudentIdAndStatus(Long studentId, String status);

    long countByTripIdAndStatus(Integer tripId, String status);
}
