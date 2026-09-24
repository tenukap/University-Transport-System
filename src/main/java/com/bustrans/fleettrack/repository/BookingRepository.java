package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUser_UserId(Long userId);
    List<Booking> findByBusTrip_TripId(Integer tripId);
    List<Booking> findByStatus(String status);
    long countByStatus(String status);
    List<Booking> findByUser_UserIdOrderByCreatedAtDesc(Long userId);
    List<Booking> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
