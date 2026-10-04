package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.LocationUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface LocationUpdateRepository extends JpaRepository<LocationUpdate, Integer> {
    List<LocationUpdate> findByTripIdOrderByRecordedAtAsc(int tripId);
    List<LocationUpdate> findByTripIdIn(Collection<Integer> tripIds);

    // One query returns the single latest ping per trip (by highest auto-increment id).
    @Query("SELECT lu FROM LocationUpdate lu WHERE lu.locationUpdateId IN " +
           "(SELECT MAX(lu2.locationUpdateId) FROM LocationUpdate lu2 WHERE lu2.tripId IN :tripIds GROUP BY lu2.tripId)")
    List<LocationUpdate> findLatestByTripIds(@Param("tripIds") Collection<Integer> tripIds);

    // For bus detail: all pings for a set of trips newest-first so Java can take top-10 per trip.
    @Query("SELECT lu FROM LocationUpdate lu WHERE lu.tripId IN :tripIds ORDER BY lu.recordedAt DESC")
    List<LocationUpdate> findByTripIdsOrderByRecordedAtDesc(@Param("tripIds") Collection<Integer> tripIds);
}
