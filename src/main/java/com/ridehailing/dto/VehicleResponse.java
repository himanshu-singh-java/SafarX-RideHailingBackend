package com.ridehailing.dto;

import com.ridehailing.model.Auto;
import com.ridehailing.model.Bike;
import com.ridehailing.model.Car;
import com.ridehailing.model.Vehicles;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hibernate.Hibernate;

@Getter
@AllArgsConstructor
public class VehicleResponse {

    private final int vehicleId;
    private final String vehicleType;
    private final String model;
    private final String categoryOrSubType;
    private final int passengerCapacity;

    public static VehicleResponse fromEntity(Vehicles vehicle) {
        if (vehicle == null) {
            return new VehicleResponse(0, "UNKNOWN", "UNKNOWN", "UNKNOWN", 0);
        }

        Vehicles v = (Vehicles) Hibernate.unproxy(vehicle);
        if (v.getVehicleCategory() != null) {
            switch (v.getVehicleCategory()) {
                case CAR -> {
                    Car car = (Car) v;
                    return new VehicleResponse(
                            v.getVehicleId(), "CAR", car.getCarModel(),
                            car.getCarCategory(), car.getPassengerCapacity()
                    );
                }
                case BIKE -> {
                    Bike bike = (Bike) v;
                    return new VehicleResponse(
                            v.getVehicleId(), "BIKE", bike.getBikeModel(),
                            bike.getBikeType(), bike.getPassengerCapacity()
                    );
                }
                case AUTO -> {
                    Auto auto = (Auto) v;
                    return new VehicleResponse(
                            v.getVehicleId(), "AUTO", auto.getAutoModel(),
                            auto.getPermitType(), auto.getPassengerCapacity()
                    );
                }
            }
        }
        return new VehicleResponse(v.getVehicleId(), "UNKNOWN", "UNKNOWN", "UNKNOWN", 0);
    }
}
