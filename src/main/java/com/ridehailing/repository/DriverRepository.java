package com.ridehailing.repository;

import com.ridehailing.model.Driver;
import com.ridehailing.model.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
@Repository
public interface DriverRepository extends JpaRepository<Driver, Integer> {

    Optional<Driver> findByUser_Email(String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Driver d SET d.driverStatus = :newStatus WHERE d.driverId = :driverId AND d.driverStatus = :expectedStatus")
    int claimDriver(@Param("driverId") int driverId,
                    @Param("newStatus") VehicleStatus newStatus,
                    @Param("expectedStatus") VehicleStatus expectedStatus);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Driver d SET d.currentLat = :lat, d.currentLng = :lng WHERE d.driverId = :id")
    int updateLocation(@Param("id") int id,
                       @Param("lat") double lat,
                       @Param("lng") double lng
                       );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Driver d SET d.driverStatus = :s WHERE d.driverId = :id AND d.driverStatus <> com.ridehailing.model.VehicleStatus.ON_RIDE")
    int updateStatusIfNotOnRide(@Param("id") int id,
                                @Param("s") VehicleStatus s);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Driver d SET d.driverStatus = com.ridehailing.model.VehicleStatus.AVAILABLE " +
           "WHERE d.driverId = :id AND d.driverStatus = com.ridehailing.model.VehicleStatus.ON_RIDE " +
           "AND NOT EXISTS (SELECT 1 FROM Rides r WHERE r.vehicle = d.vehicle " +
           "AND r.rideStatus IN (com.ridehailing.model.RideStatus.ACCEPTED, com.ridehailing.model.RideStatus.IN_PROGRESS))")
    int recoverStuckOnRide(@Param("id") int id);

}
