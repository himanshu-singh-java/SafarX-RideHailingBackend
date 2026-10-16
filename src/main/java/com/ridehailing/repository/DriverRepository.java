package com.ridehailing.repository;

import com.ridehailing.model.Driver;
import com.ridehailing.model.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Integer> {

    Optional<Driver> findByUser_Email(String email);

    @Modifying
    @Query("UPDATE Driver d SET d.driverStatus = :newStatus WHERE d.driverId = :driverId AND d.driverStatus = :expectedStatus")
    int claimDriver(@Param("driverId") int driverId,
                    @Param("newStatus") VehicleStatus newStatus,
                    @Param("expectedStatus") VehicleStatus expectedStatus);

}
