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
            value = "SELECT v.* FROM vehicles v " +
                    "INNER JOIN drivers d ON v.vehicle_id = d.vehicle_id " +
                    "WHERE d.status = 'AVAILABLE' " +
                    "AND (" +
                    "  (UPPER(:vehicleType) = 'CAR' AND EXISTS (SELECT 1 FROM cars c WHERE c.vehicle_id = v.vehicle_id)) OR " +
                    "  (UPPER(:vehicleType) = 'BIKE' AND EXISTS (SELECT 1 FROM bikes b WHERE b.vehicle_id = v.vehicle_id)) OR " +
                    "  (UPPER(:vehicleType) = 'AUTO' AND EXISTS (SELECT 1 FROM autos a WHERE a.vehicle_id = v.vehicle_id))" +
                    ") " +
                    "AND (6371 * acos(cos(radians(:pickupLat)) * cos(radians(d.current_latitude)) " +
                    "* cos(radians(d.current_longitude) - radians(:pickupLng)) " +
                    "+ sin(radians(:pickupLat)) * sin(radians(d.current_latitude)))) <= :radius " +
                    "ORDER BY (6371 * acos(cos(radians(:pickupLat)) * cos(radians(d.current_latitude)) " +
                    "* cos(radians(d.current_longitude) - radians(:pickupLng)) " +
                    "+ sin(radians(:pickupLat)) * sin(radians(d.current_latitude)))) ASC " +
                    "LIMIT 1", nativeQuery = true
    )
    Vehicles findNearestAvailableVehicle(@Param("vehicleType") String vehicleType,
                                         @Param("pickupLat") Double pickupLat,
                                         @Param("pickupLng") Double pickupLng,
                                         @Param("radius") int radius);

}
