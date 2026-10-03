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

    /** Non-CANCELLED bookings for a trip — used to find used seats and detect duplicates. */
    List<Booking> findByBusTrip_TripIdAndStatusNot(Integer tripId, String status);

    // Used by UserService.deleteUser: if a student has any bookings, hard-delete is blocked.
    boolean existsByUser_UserId(Long userId);

    /** Non-CANCELLED bookings for a student whose trip date falls in [firstDay, nextMonth). */
    @Query("SELECT b FROM Booking b WHERE b.user.userId = :userId " +
           "AND b.busTrip.tripDate >= :firstDay AND b.busTrip.tripDate < :nextMonth " +
           "AND b.status <> 'CANCELLED'")
    List<Booking> findChargeableByUserAndMonth(@Param("userId") Long userId,
                                               @Param("firstDay") LocalDate firstDay,
                                               @Param("nextMonth") LocalDate nextMonth);
}
