package com.ridehailing.repository;

import com.ridehailing.model.RideStatus;
import com.ridehailing.model.Rides;
import com.ridehailing.model.Vehicles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT r FROM Rides r WHERE r.vehicle.vehicleId = :vehicleId AND r.rideStatus IN('IN_PROGRESS', 'ACCEPTED')")
    Optional<Rides> findActiveRideByVehicleId(@Param("vehicleId") int vehicleId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Rides r SET r.rideStatus = :acceptedStatus, r.vehicle = :vehicle," +
            " r.startTime = CURRENT_TIMESTAMP WHERE r.rideId = :id AND r.rideStatus = :requestedStatus")
    int claimRideAtDB(@Param("id") int id,
                      @Param("vehicle")Vehicles vehicle,
                      @Param("acceptedStatus") RideStatus acceptedStatus,
                      @Param("requestedStatus") RideStatus requestedStatus
                      );
}