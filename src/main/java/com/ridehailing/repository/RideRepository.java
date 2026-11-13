package com.ridehailing.repository;

import com.ridehailing.model.Driver;
import com.ridehailing.model.RideStatus;
import com.ridehailing.model.Rides;
import com.ridehailing.model.Vehicles;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    @Query("SELECT r FROM Rides r LEFT JOIN FETCH r.vehicle v LEFT JOIN FETCH r.driver d LEFT JOIN FETCH d.user u WHERE r.rider.riderID = :riderId ORDER BY r.rideId DESC")
    List<Rides> findByRider_RiderIDOrderByRideIdDesc(@Param("riderId") int riderId);

    @Query("SELECT r FROM Rides r LEFT JOIN FETCH r.vehicle v LEFT JOIN FETCH r.driver d LEFT JOIN FETCH d.user u ORDER BY r.rideId DESC")
    List<Rides> findAllByOrderByRideIdDesc();

    boolean existsByRider_RiderIDAndRideStatusIn(int riderId, Collection<RideStatus> statuses);

    boolean existsByRider_RiderIDAndRideStatus(int riderID, RideStatus rideStatus);

    List<Rides> findByRideStatusAndLastBroadcastAtBefore(RideStatus rideStatus, LocalDateTime cutOffTime);

    @Query("SELECT r FROM Rides r WHERE r.vehicle.vehicleId = :vehicleId AND r.rideStatus IN (com.ridehailing.model.RideStatus.IN_PROGRESS, com.ridehailing.model.RideStatus.ACCEPTED) ORDER BY r.rideId DESC")
    List<Rides> findActiveRidesByVehicleId(@Param("vehicleId") int vehicleId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Rides r SET r.rideStatus = :acceptedStatus, r.vehicle = :vehicle, r.driver = :driver " +
            "WHERE r.rideId = :id AND r.rideStatus = :requestedStatus")
    int claimRideAtDB(@Param("id") int id,
                      @Param("vehicle") Vehicles vehicle,
                      @Param("driver") Driver driver,
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Rides r WHERE r.rideId = :id")
    Optional<Rides> findByIdForUpdate(@Param("id") int id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Rides r SET r.currentRadiusIndex = r.currentRadiusIndex + 1, r.lastBroadcastAt = :now " +
            "WHERE r.rideId = :rideId AND r.rideStatus = com.ridehailing.model.RideStatus.REQUESTED " +
            "AND r.lastBroadcastAt <= :cutoff")
    int incrementRadiusAndClaimTimeout(@Param("rideId") int rideId,
                                       @Param("now") LocalDateTime now,
                                       @Param("cutoff") LocalDateTime cuttoff
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM ride_notified_drivers WHERE ride_id = :rideId", nativeQuery = true)
    void deleteNotifiedDriversByRideId(@Param("rideId") int rideId);
}