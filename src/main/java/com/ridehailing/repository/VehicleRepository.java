package com.ridehailing.repository;

import com.ridehailing.model.Vehicles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicles, Integer> {

    @Query("SELECT b FROM Bike b")
    List<Vehicles> findAllBikes();

    @Query("SELECT c FROM Car c")
    List<Vehicles> findAllCars();

    @Query("SELECT a FROM Auto a")
    List<Vehicles> findAllAutos();


    @Query(
            "SELECT v FROM Vehicles v " +
                    "JOIN v.driver d " +
                    "WHERE d.driverStatus = com.ridehailing.model.VehicleStatus.AVAILABLE " +
                    "AND (" +
                    "  (UPPER(:vehicleType) = 'CAR' AND TYPE(v) = Car) OR " +
                    "  (UPPER(:vehicleType) = 'BIKE' AND TYPE(v) = Bike) OR " +
                    "  (UPPER(:vehicleType) = 'AUTO' AND TYPE(v) = Auto)" +
                    ") " +
                    "AND d.driverId NOT IN (SELECT rsl.driverId FROM RideSkippedLog rsl WHERE rsl.rideId = :rideId) " +
                    "AND (6371 * acos(cos(radians(:pickupLat)) * cos(radians(d.currentLat)) " +
                    "* cos(radians(d.currentLng) - radians(:pickupLng)) " +
                    "+ sin(radians(:pickupLat)) * sin(radians(d.currentLat)))) <= :radius"

    )
    List<Vehicles> findAllAvailableVehiclesForRide(@Param("vehicleType") String vehicleType,
                                                   @Param("pickupLat") Double pickupLat,
                                                   @Param("pickupLng") Double pickupLng,
                                                   @Param("radius") int radius,
                                                   @Param("rideId") int rideId);

    Vehicles findByDriver_DriverId(int driverId);
}
