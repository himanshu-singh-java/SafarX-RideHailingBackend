package com.ridehailing.service;

import com.ridehailing.dto.DriverOnboardingRequest;
import com.ridehailing.exception.RideConflictException;
import com.ridehailing.model.*;
import com.ridehailing.repository.DriverRepository;
import com.ridehailing.repository.UserRepository;
import com.ridehailing.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class DriverService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final PasswordEncoder passwordEncoder;

    public DriverService(UserRepository userRepository,
                         DriverRepository driverRepository,
                         VehicleRepository vehicleRepository,
                         PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.passwordEncoder = passwordEncoder;
    }


    public Vehicles buildVehicle(DriverOnboardingRequest request, Driver driver){
        if(request.getVehicleType() == null){
            throw new IllegalArgumentException("Vehicle type cannot be null");
        }

        String type = request.getVehicleType().toUpperCase();
        Vehicles vehicle = null;

        switch (type){

            case "CAR":
                Car car = new Car();
                car.setPlateNumber(request.getPlateNumber());
                car.setCarModel(request.getCarModel());
                car.setCarCategory(request.getCarCategory());
                car.setPassengerCapacity(request.getCarPassengerCapacity() != null ? request.getCarPassengerCapacity() : 4);
                car.setIsAcAvailable(request.getIsAcAvailable() != null ? request.getIsAcAvailable() : false);
                car.setHasLuggageCarrier(request.getHasLuggageCarrier() != null ? request.getHasLuggageCarrier() : false);
                car.setHasWifiOrScreen(request.getHasWifiOrScreen() != null ? request.getHasWifiOrScreen() : false);
                vehicle = car;
                break;

            case "BIKE":
                Bike bike = new Bike();
                bike.setPlateNumber(request.getPlateNumber());
                bike.setBikeModel(request.getBikeModel());
                bike.setBikeType(request.getBikeType());
                bike.setPassengerCapacity(1);
                bike.setHasExtraHelmet(request.getHasExtraHelmet() != null ? request.getHasExtraHelmet() : false);
                bike.setIsParcelDeliveryEnabled(request.getIsParcelDeliveryEnabled() != null ? request.getIsParcelDeliveryEnabled() : false);
                vehicle = bike;
                break;

            case "AUTO":
                Auto auto = new Auto();
                auto.setPlateNumber(request.getPlateNumber());
                auto.setAutoModel(request.getAutoModel());
                auto.setPermitType(request.getPermitType());
                auto.setPassengerCapacity(request.getAutoPassengerCapacity() != null ? request.getAutoPassengerCapacity() : 3);
                auto.setIsSharedAuto(request.getIsSharedAuto() != null ? request.getIsSharedAuto() : false);
                vehicle = auto;
                break;

            default:
                throw new IllegalArgumentException("Error: Invalid Vehicle Type provided!");
        }
        vehicle.setDriver(driver);
        return vehicle;
    }

    @Transactional
    public Driver onBoardDriver(DriverOnboardingRequest request){

        Optional<Users> existingDriver = userRepository.findByEmail(request.getEmail());

        if(existingDriver.isPresent()){
            throw new RideConflictException("Error: This email is already registered! Please use a different email.");
        }

        Users user = new Users();
        user.setEmail(request.getEmail());
        user.setName(request.getFullName() != null ? request.getFullName().trim() : null);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_DRIVER);
        user = userRepository.save(user);

        Driver driver = new Driver();
        driver.setUser(user);
        driver.setDriverName(request.getFullName());
        driver.setRating(5.0);
        driver.setCurrentLat(request.getLatitude());
        driver.setCurrentLng(request.getLongitude());
        driver.setDriverStatus(VehicleStatus.AVAILABLE);

        Vehicles vehicle = buildVehicle(request, driver);
        vehicle = vehicleRepository.save(vehicle);
        driver.setVehicle(vehicle);

        return driverRepository.save(driver);
    }
}
