package com.ridehailing.repository;

import com.ridehailing.model.Driver;
import com.ridehailing.model.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    @Modifying
    @Query("UPDATE Driver d SET d.driverStatus = :newStatus WHERE d.driverId = :driverId AND d.driverStatus = :expectedStatus")
    int claimDriver(@Param("driverId") Long driverId,
                    @Param("newStatus") VehicleStatus newStatus,
                    @Param("expectedStatus") VehicleStatus expectedStatus);

}
