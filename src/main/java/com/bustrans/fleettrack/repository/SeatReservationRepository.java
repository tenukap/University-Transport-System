package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {
    List<SeatReservation> findByBooking_Id(Long bookingId);
    List<SeatReservation> findByBus_BusId(Long busId);

    void deleteByBooking_Id(Long bookingId);

    @Query("SELECT COUNT(sr) FROM SeatReservation sr WHERE sr.bus.busId = :busId")
    long countByBusId(@Param("busId") Long busId);

    @Query("SELECT MAX(sr.seatNumber) FROM SeatReservation sr WHERE sr.bus.busId = :busId")
    Integer findMaxSeatNumberByBusId(@Param("busId") Long busId);

    /** Bulk-deletes all seat reservations whose booking belongs to a given trip (used when the trip is cancelled). */
    @Transactional
    @Modifying
    @Query("DELETE FROM SeatReservation sr WHERE sr.booking.busTrip.tripId = :tripId")
    void deleteByTripId(@Param("tripId") Integer tripId);
}
