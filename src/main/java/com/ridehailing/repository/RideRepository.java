package com.ridehailing.repository;

import com.ridehailing.model.RideStatus;
import com.ridehailing.model.Rides;
import com.ridehailing.model.Vehicles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;


@Repository
public interface RideRepository extends JpaRepository<Rides, Integer> {

    List<Rides> findByRider_RiderIDOrderByRideIdDesc(int riderId);

    List<Rides> findAllByOrderByRideIdDesc();

    boolean existsByRider_RiderIDAndRideStatusIn(int riderId, Collection<RideStatus> statuses);

    boolean existsByRider_RiderIDAndRideStatus(int riderID, RideStatus rideStatus);

    List<Rides> findByRideStatusAndLastBroadcastAtBefore(RideStatus rideStatus, LocalDateTime cutOffTime);

    @Query("SELECT r FROM Rides r WHERE r.vehicle.vehicleId = :vehicleId AND r.rideStatus IN (com.ridehailing.model.RideStatus.IN_PROGRESS, com.ridehailing.model.RideStatus.ACCEPTED)")
    Optional<Rides> findActiveRideByVehicleId(@Param("vehicleId") int vehicleId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Rides r SET r.rideStatus = :acceptedStatus, r.vehicle = :vehicle, r.fare = :fare, " +
            " r.startTime = CURRENT_TIMESTAMP WHERE r.rideId = :id AND r.rideStatus = :requestedStatus")
    int claimRideAtDB(@Param("id") int id,
                      @Param("vehicle")Vehicles vehicle,
                      @Param("fare") double fare,
                      @Param("acceptedStatus") RideStatus acceptedStatus,
                      @Param("requestedStatus") RideStatus requestedStatus
                      );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Rides r SET r.fare = :fare, r.currentRadiusIndex = :radiusIndex, r.lastBroadcastAt = :broadcastTime WHERE r.rideId = :rideId AND r.rideStatus = com.ridehailing.model.RideStatus.REQUESTED")
    int updateBroadcastStateAtDB(
            @Param("rideId") int rideId,
            @Param("fare") double fare,
            @Param("radiusIndex") int radiusIndex,
            @Param("broadcastTime") LocalDateTime broadcastTime
    );
}