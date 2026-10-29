package com.ridehailing.repository;

import com.ridehailing.model.RideNotifiedDriver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RideNotifiedDriverRepository extends JpaRepository<RideNotifiedDriver, Integer> {
    List<RideNotifiedDriver> findByRideId(int rideId);

    boolean existsByRideIdAndDriverId(int rideId, int driverId);
}
