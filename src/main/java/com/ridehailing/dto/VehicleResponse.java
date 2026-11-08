package com.ridehailing.dto;

import com.ridehailing.model.Auto;
import com.ridehailing.model.Bike;
import com.ridehailing.model.Car;
import com.ridehailing.model.Vehicles;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleResponse {

    private int vehicleId;
    private String vehicleType;
    private String model;
    private String categoryOrSubType;
    private int passengerCapacity;

    public static VehicleResponse fromEntity(Vehicles vehicle) {
        VehicleResponse dto = new VehicleResponse();
        dto.setVehicleId(vehicle.getVehicleId());

        if (vehicle instanceof Car car) {
            dto.setVehicleType("CAR");
            dto.setModel(car.getCarModel());
            dto.setCategoryOrSubType(car.getCarCategory());
            dto.setPassengerCapacity(car.getPassengerCapacity());
        } else if (vehicle instanceof Bike bike) {
            dto.setVehicleType("BIKE");
            dto.setModel(bike.getBikeModel());
            dto.setCategoryOrSubType(bike.getBikeType());
            dto.setPassengerCapacity(bike.getPassengerCapacity());
        } else if (vehicle instanceof Auto auto) {
            dto.setVehicleType("AUTO");
            dto.setModel(auto.getAutoModel());
            dto.setCategoryOrSubType(auto.getPermitType());
            dto.setPassengerCapacity(auto.getPassengerCapacity());
        }
        return dto;
    }
}
