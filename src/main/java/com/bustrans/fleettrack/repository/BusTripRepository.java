package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BusTripRepository extends JpaRepository<BusTrip, Integer> {
    List<BusTrip> findByTripDate(LocalDate tripDate);
    List<BusTrip> findByPickupLocation_LocationId(Integer locationId);
    List<BusTrip> findByDropLocation_LocationId(Integer locationId);
    List<BusTrip> findByTripStatus(String tripStatus);

    /** Acquires a PESSIMISTIC_WRITE lock on the row so concurrent bookings are serialized. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bt FROM BusTrip bt WHERE bt.tripId = :id")
    Optional<BusTrip> findByIdForUpdate(@Param("id") Integer id);

    // LocalTime cannot be used as a bound parameter against SQL Server TIME columns
    // (JDBC driver maps it as datetime, causing a type-incompatibility error in >).
    // Same-day time filtering is done in TripService after the query returns.
    @Query("SELECT bt FROM BusTrip bt " +
           "WHERE bt.tripStatus = 'Scheduled' " +
           "AND bt.tripDate >= :today " +
           "ORDER BY bt.tripDate, bt.startTime")
    List<BusTrip> findAvailableTrips(@Param("today") LocalDate today);
}
