package com.ridehailing.mapper;

import com.ridehailing.dto.RideRequest;
import com.ridehailing.dto.RideResponse;
import com.ridehailing.model.Auto;
import com.ridehailing.model.Bike;
import com.ridehailing.model.Car;
import com.ridehailing.model.Driver;
import com.ridehailing.model.Rides;
import com.ridehailing.model.Vehicles;

public class RideMapper {

    public static Rides mapToEntity(RideRequest request){
        Rides rides = new Rides();
        rides.setPickupLat(request.getPickupLat());
        rides.setPickupLng(request.getPickupLng());
        rides.setDropLat(request.getDropLat());
        rides.setDropLng(request.getDropLng());
        return rides;
    }

    public static RideResponse mapToResponse(Rides rides){
        if (rides == null) return null;

        Integer driverId = null;
        String driverName = null;
        Double driverRating = null;
        String driverPhone = null;
        String vehicleNumber = null;
        String vehicleModel = null;
        String vehicleType = null;
        RideResponse.DriverDetails driverDetails = null;

        Vehicles v = rides.getVehicle();
        if (v != null) {
            vehicleNumber = v.getPlateNumber();
            if (v instanceof Car car) {
                vehicleType = "CAR";
                vehicleModel = car.getCarModel();
            } else if (v instanceof Bike bike) {
                vehicleType = "BIKE";
                vehicleModel = bike.getBikeModel();
            } else if (v instanceof Auto auto) {
                vehicleType = "AUTO";
                vehicleModel = auto.getAutoModel();
            } else {
                vehicleType = rides.getVehicleType();
            }

            Driver d = v.getDriver();
            if (d != null) {
                driverId = d.getDriverId();
                driverRating = d.getRating();
                if (d.getDriverName() != null && !d.getDriverName().isBlank()) {
                    driverName = d.getDriverName().trim();
                } else if (d.getUser() != null && d.getUser().getName() != null && !d.getUser().getName().isBlank()) {
                    driverName = d.getUser().getName().trim();
                } else if (d.getUser() != null && d.getUser().getEmail() != null) {
                    driverName = d.getUser().getEmail().split("@")[0];
                }

                if (d.getUser() != null && d.getUser().getEmail() != null) {
                    driverPhone = d.getUser().getEmail();
                }

                driverDetails = new RideResponse.DriverDetails(
                        driverId,
                        driverId,
                        driverName,
                        driverName,
                        driverRating,
                        driverPhone,
                        vehicleNumber,
                        vehicleNumber,
                        vehicleModel,
                        vehicleModel,
                        vehicleType
                );
            }
        }

        return new RideResponse(
                rides.getRideId(),
                rides.getPickupLat(),
                rides.getPickupLng(),
                rides.getDropLat(),
                rides.getDropLng(),
                rides.getFare() != null ? rides.getFare() : 0.0,
                rides.getRideStatus() != null ? rides.getRideStatus().name() : "UNKNOWN",
                driverId,
                driverName,
                driverRating,
                driverPhone,
                vehicleNumber,
                vehicleNumber,
                vehicleModel,
                vehicleType,
                driverDetails
        );
    }
}
