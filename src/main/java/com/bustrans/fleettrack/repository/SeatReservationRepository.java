package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.SeatReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {

    Optional<SeatReservation> findByBookingId(Long bookingId);

    @Query("SELECT COUNT(sr) FROM SeatReservation sr "
            + "WHERE sr.bus.busId = :busId AND sr.booking.tripId = :tripId")
    long countByBusIdAndBookingTripId(@Param("busId") Long busId,
                                      @Param("tripId") Integer tripId);

    @Query("SELECT MAX(sr.seatNumber) FROM SeatReservation sr "
            + "WHERE sr.bus.busId = :busId AND sr.booking.tripId = :tripId")
    Optional<Integer> findMaxSeatNumberByBusIdAndBookingTripId(@Param("busId") Long busId,
                                                               @Param("tripId") Integer tripId);
}
