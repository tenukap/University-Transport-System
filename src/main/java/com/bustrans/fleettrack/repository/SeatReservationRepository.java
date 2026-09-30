package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
