package com.ridehailing.dto;

import com.ridehailing.model.Auto;
import com.ridehailing.model.Bike;
import com.ridehailing.model.Car;
import com.ridehailing.model.Vehicles;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VehicleResponse {

    private final int vehicleId;
    private final String vehicleType;
    private final String model;
    private final String categoryOrSubType;
    private final int passengerCapacity;

    public static VehicleResponse fromEntity(Vehicles vehicle) {

        if (vehicle instanceof Car car) {
            return new VehicleResponse(
                    vehicle.getVehicleId(), "CAR", car.getCarModel(),
                    car.getCarCategory(), car.getPassengerCapacity()
            );
        } else if (vehicle instanceof Bike bike) {
            return new VehicleResponse(
                    vehicle.getVehicleId(), "BIKE", bike.getBikeModel(),
                    bike.getBikeType(), bike.getPassengerCapacity()
            );
        } else if (vehicle instanceof Auto auto) {
            return new VehicleResponse(
                    vehicle.getVehicleId(), "AUTO", auto.getAutoModel(),
                    auto.getPermitType(), auto.getPassengerCapacity()
            );
        }
        return new VehicleResponse(vehicle.getVehicleId(), "UNKNOWN", "UNKNOWN", "UNKNOWN", 0);
    }
}
