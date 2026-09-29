package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BusTripRepository extends JpaRepository<BusTrip, Integer> {
    List<BusTrip> findByTripDate(LocalDate tripDate);
    List<BusTrip> findByPickupLocation_LocationId(Integer locationId);
    List<BusTrip> findByDropLocation_LocationId(Integer locationId);
    List<BusTrip> findByTripStatus(String tripStatus);

    @Query("SELECT bt FROM BusTrip bt " +
           "WHERE bt.tripStatus = 'Scheduled' " +
           "AND (bt.tripDate > :today " +
           "     OR (bt.tripDate = :today AND bt.startTime > :nowTime)) " +
           "ORDER BY bt.tripDate, bt.startTime")
    List<BusTrip> findAvailableTrips(@Param("today") LocalDate today,
                                     @Param("nowTime") LocalTime nowTime);
}
