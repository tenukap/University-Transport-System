package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /** All non-CANCELLED bookings for a student — used by invoice sync to find all billing months. */
    List<Booking> findByUser_UserIdAndStatusNot(Long userId, String status);

    /** Student tracking: active bookings for upcoming/recent trips in a date window. */
    @Query("SELECT b FROM Booking b WHERE b.user.userId = :userId " +
           "AND b.busTrip.tripDate BETWEEN :from AND :to " +
           "AND b.status <> 'CANCELLED' " +
           "AND b.busTrip.tripStatus <> 'Cancelled' " +
           "ORDER BY b.busTrip.tripDate ASC, b.busTrip.startTime ASC")
    List<Booking> findActiveBookingsForTracking(@Param("userId") Long userId,
                                                @Param("from") LocalDate from,
                                                @Param("to") LocalDate to);

    /** CONFIRMED bookings for a student whose trip date is today or earlier.
     *  startTime filtering is done in Java — SQL Server JDBC maps LocalTime as datetime,
     *  causing a type-incompatibility error when compared against a TIME column. */
    @Query("SELECT b FROM Booking b WHERE b.user.userId = :userId AND b.status = 'CONFIRMED' " +
           "AND b.busTrip.tripDate <= :today")
    List<Booking> findDepartedConfirmedBookings(@Param("userId") Long userId,
                                                @Param("today") LocalDate today);

    /** Bulk-cancels all CONFIRMED or PENDING bookings for a trip. Returns the number of rows updated.
     *  Invoices are recalculated from non-cancelled bookings on the next read, so charges disappear automatically. */
    @Modifying
    @Query("UPDATE Booking b SET b.status = 'CANCELLED' " +
           "WHERE b.busTrip.tripId = :tripId AND b.status IN ('CONFIRMED', 'PENDING')")
    int cancelBookingsForTrip(@Param("tripId") Integer tripId);
}
