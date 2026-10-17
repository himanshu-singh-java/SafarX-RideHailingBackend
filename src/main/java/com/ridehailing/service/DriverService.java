package com.ridehailing.service;

import com.ridehailing.dto.DriverOnboardingRequest;
import com.ridehailing.exception.ResourceNotFoundException;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public String onBoardDriver(DriverOnboardingRequest request){

        Users user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if(user == null){

            user = new Users();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(Role.ROLE_DRIVER);
            user = userRepository.save(user);
        }
        else{
            user.setRole(Role.ROLE_DRIVER);
            user = userRepository.save(user);
        }

        Optional<Driver> existingDriver = driverRepository.findByUser_Email(request.getEmail());
        if(existingDriver.isPresent()){
            throw new RideConflictException("Error: Driver profile already exists for this email!");
        }

        Vehicles vehicle = null;

        String type = request.getVehicleType().toUpperCase();

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
                vehicle = vehicleRepository.save(car);
                break;

            case "BIKE":
                Bike bike = new Bike();
                bike.setPlateNumber(request.getPlateNumber());
                bike.setBikeModel(request.getBikeModel());
                bike.setBikeType(request.getBikeType());
                bike.setPassengerCapacity(1);
                bike.setHasExtraHelmet(request.getHasExtraHelmet() != null ? request.getHasExtraHelmet() : false);
                bike.setIsParcelDeliveryEnabled(request.getIsParcelDeliveryEnabled() != null ? request.getIsParcelDeliveryEnabled() : false);
                vehicle = vehicleRepository.save(bike);
                break;

            case "AUTO":
                Auto auto = new Auto();
                auto.setPlateNumber(request.getPlateNumber());
                auto.setAutoModel(request.getAutoModel());
                auto.setPermitType(request.getPermitType());
                auto.setPassengerCapacity(request.getAutoPassengerCapacity() != null ? request.getAutoPassengerCapacity() : 3);
                auto.setIsSharedAuto(request.getIsSharedAuto() != null ? request.getIsSharedAuto() : false);
                vehicle = vehicleRepository.save(auto);
                break;

            default:
                throw new ResourceNotFoundException("Error: Invalid Vehicle Type provided!");
        }

        Driver driver = new Driver();
        driver.setUser(user);
        driver.setVehicle(vehicle);
        driver.setDriverName(request.getFullName());
        driver.setRating(5.0);
        driver.setDriverStatus(VehicleStatus.OFFLINE);

        driverRepository.save(driver);

        return "Driver & Vehicle Onboarded Successfully!";
    }
}
