package com.ridehailing.repository;

import com.ridehailing.model.RideStatus;
import com.ridehailing.model.Rides;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


@Repository
public interface RideRepository extends JpaRepository<Rides, Integer> {

    List<Rides> findByRider_RiderIDOrderByRideIdDesc(int riderId);

    List<Rides> findAllByOrderByRideIdDesc();

    boolean existsByRider_RiderIDAndRideStatusIn(int riderId, Collection<RideStatus> statuses);

    boolean existsByRider_RiderIDAndRideStatus(int riderID, RideStatus rideStatus);

    @Query("SELECT r FROM Rides r WHERE r.vehicle.vehicleId = :vehicleId AND r.rideStatus IN('IN_PROGRESS', 'ACCEPTED')")
    Optional<Rides> findActiveRideByVehicleId(@Param("vehicleId") int vehicleId);
}