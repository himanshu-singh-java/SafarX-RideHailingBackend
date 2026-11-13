package com.ridehailing.service;

import com.ridehailing.dto.DriverOnboardingRequest;
import com.ridehailing.dto.VehicleRegistrationRequest;
import com.ridehailing.exception.ResourceNotFoundException;
import com.ridehailing.exception.RideConflictException;
import com.ridehailing.model.*;
import com.ridehailing.repository.DriverRepository;
import com.ridehailing.repository.UserRepository;
import com.ridehailing.repository.VehicleRepository;
import org.hibernate.Hibernate;
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

    private VehicleCategory parseCategory(String rawVehicleType){
        if(rawVehicleType == null || rawVehicleType.isBlank()){
            throw new IllegalArgumentException("Vehicle category cannot be null or blank");
        }
        try {
            return VehicleCategory.valueOf(rawVehicleType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Vehicle Category: " + rawVehicleType);
        }
    }

    private String requireNonBlank(String value, String fieldName){
        if(value == null || value.isBlank()){
            throw new IllegalArgumentException(fieldName + " is required and cannot be blank");
        }
        return value.trim();
    }

    public Vehicles buildVehicle(DriverOnboardingRequest request, Driver driver){

        VehicleCategory category = parseCategory(request.getVehicleType());
        String plateNumber = requireNonBlank(request.getPlateNumber(), "Plate number");

        Vehicles vehicle;

        switch (category){

            case CAR -> {
                Car car = new Car();
                car.setPlateNumber(plateNumber);
                car.setCarModel(requireNonBlank(request.getCarModel(), "Car model"));
                car.setCarCategory(request.getCarCategory() != null ? request.getCarCategory().trim() : "SEDAN");
                car.setPassengerCapacity(request.getCarPassengerCapacity() != null ? request.getCarPassengerCapacity() : 4);
                car.setIsAcAvailable(Boolean.TRUE.equals(request.getIsAcAvailable()));
                car.setHasLuggageCarrier(Boolean.TRUE.equals(request.getHasLuggageCarrier()));
                car.setHasWifiOrScreen(Boolean.TRUE.equals(request.getHasWifiOrScreen()));
                vehicle = car;
            }

            case BIKE -> {
                Bike bike = new Bike();
                bike.setPlateNumber(plateNumber);
                bike.setBikeModel(requireNonBlank(request.getBikeModel(), "Bike model"));
                bike.setBikeType(request.getBikeType() != null ? request.getBikeType().trim() : "COMMUTER");
                bike.setPassengerCapacity(1);
                bike.setHasExtraHelmet(Boolean.TRUE.equals(request.getHasExtraHelmet()));
                bike.setIsParcelDeliveryEnabled(Boolean.TRUE.equals(request.getIsParcelDeliveryEnabled()));
                vehicle = bike;
            }

            case AUTO -> {
                Auto auto = new Auto();
                auto.setPlateNumber(plateNumber);
                auto.setAutoModel(requireNonBlank(request.getAutoModel(), "Auto model"));
                auto.setPermitType(request.getPermitType() != null ? request.getPermitType().trim() : "CITY");
                auto.setPassengerCapacity(request.getAutoPassengerCapacity() != null ? request.getAutoPassengerCapacity() : 3);
                auto.setIsSharedAuto(Boolean.TRUE.equals(request.getIsSharedAuto()));
                vehicle = auto;
            }
            default -> throw new IllegalArgumentException("Unsupported Vehicle Category: " + category);
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

        String fullName = requireNonBlank(request.getFullName(), "Full name");
        String phone = requireNonBlank(request.getPhone(), "Phone number");

        Users user = new Users();
        user.setEmail(request.getEmail().trim());
        user.setName(fullName);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_DRIVER);
        user = userRepository.save(user);

        Driver driver = new Driver();
        driver.setUser(user);
        driver.setDriverName(fullName);
        driver.setPhone(phone);
        driver.setRating(5.0);
        driver.setCurrentLat(request.getLatitude());
        driver.setCurrentLng(request.getLongitude());
        driver.setDriverStatus(VehicleStatus.AVAILABLE);

        Vehicles vehicle = buildVehicle(request, driver);
        vehicle = vehicleRepository.save(vehicle);
        driver.setVehicle(vehicle);

        return driverRepository.save(driver);
    }

    @Transactional
    public Vehicles registerVehicleForDriver(String email, VehicleRegistrationRequest request) {
        Driver driver = driverRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        if (driver.getVehicle() != null) {
            throw new RideConflictException("Vehicle is already registered for this driver.");
        }

        Vehicles vehicle = buildVehicle(request.toOnboardingRequest(), driver);
        vehicle = vehicleRepository.save(vehicle);
        driver.setVehicle(vehicle);
        driverRepository.save(driver);

        driverRepository.updateStatusIfNotOnRide(driver.getDriverId(), VehicleStatus.AVAILABLE);
        return vehicle;
    }

    @Transactional
    public Vehicles updateDriverVehicle(String email, VehicleRegistrationRequest request) {
        Driver driver = driverRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        if (driver.getDriverStatus() == VehicleStatus.ON_RIDE) {
            throw new RideConflictException("Cannot change vehicle details while on an active ride.");
        }

        DriverOnboardingRequest onboardingReq = request.toOnboardingRequest();
        VehicleCategory targetCategory = parseCategory(onboardingReq.getVehicleType());
        String newPlate = requireNonBlank(onboardingReq.getPlateNumber(), "Plate number");

        Vehicles existingProxy = driver.getVehicle();
        if (existingProxy != null) {
            Vehicles existing = (Vehicles) Hibernate.unproxy(existingProxy);

            if (existing.getVehicleCategory() == targetCategory) {
                existing.setPlateNumber(newPlate);
                switch (targetCategory) {
                    case CAR -> {
                        Car car = (Car) existing;
                        car.setCarModel(requireNonBlank(onboardingReq.getCarModel(), "Car model"));
                        if (onboardingReq.getCarCategory() != null) {
                            car.setCarCategory(onboardingReq.getCarCategory().trim());
                        }
                        if (onboardingReq.getCarPassengerCapacity() != null) {
                            car.setPassengerCapacity(onboardingReq.getCarPassengerCapacity());
                        }
                        if (onboardingReq.getIsAcAvailable() != null) {
                            car.setIsAcAvailable(onboardingReq.getIsAcAvailable());
                        }
                        if (onboardingReq.getHasLuggageCarrier() != null) {
                            car.setHasLuggageCarrier(onboardingReq.getHasLuggageCarrier());
                        }
                        if (onboardingReq.getHasWifiOrScreen() != null) {
                            car.setHasWifiOrScreen(onboardingReq.getHasWifiOrScreen());
                        }
                    }
                    case BIKE -> {
                        Bike bike = (Bike) existing;
                        bike.setBikeModel(requireNonBlank(onboardingReq.getBikeModel(), "Bike model"));
                        if (onboardingReq.getBikeType() != null) {
                            bike.setBikeType(onboardingReq.getBikeType().trim());
                        }
                        if (onboardingReq.getHasExtraHelmet() != null) {
                            bike.setHasExtraHelmet(onboardingReq.getHasExtraHelmet());
                        }
                        if (onboardingReq.getIsParcelDeliveryEnabled() != null) {
                            bike.setIsParcelDeliveryEnabled(onboardingReq.getIsParcelDeliveryEnabled());
                        }
                    }
                    case AUTO -> {
                        Auto auto = (Auto) existing;
                        auto.setAutoModel(requireNonBlank(onboardingReq.getAutoModel(), "Auto model"));
                        if (onboardingReq.getPermitType() != null) {
                            auto.setPermitType(onboardingReq.getPermitType().trim());
                        }
                        if (onboardingReq.getAutoPassengerCapacity() != null) {
                            auto.setPassengerCapacity(onboardingReq.getAutoPassengerCapacity());
                        }
                        if (onboardingReq.getIsSharedAuto() != null) {
                            auto.setIsSharedAuto(onboardingReq.getIsSharedAuto());
                        }
                    }
                }
                return vehicleRepository.save(existing);
            }

            if (existing.getPlateNumber() != null && existing.getPlateNumber().equalsIgnoreCase(newPlate)) {
                throw new RideConflictException("When switching vehicle category, please provide the new vehicle's unique registration plate number.");
            }

            existing.setDriver(null);
            driver.setVehicle(null);
            driverRepository.saveAndFlush(driver);
            vehicleRepository.saveAndFlush(existing);
        }

        Vehicles newVehicle = buildVehicle(onboardingReq, driver);
        newVehicle = vehicleRepository.save(newVehicle);
        driver.setVehicle(newVehicle);
        driverRepository.save(driver);

        return newVehicle;
    }

    @Transactional
    public String updateDriverStatus(String email, VehicleStatus status) {
        if (status == VehicleStatus.ON_RIDE) {
            throw new IllegalArgumentException("Cannot manually set status to ON_RIDE.");
        }

        Driver driver = driverRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        int updated = driverRepository.updateStatusIfNotOnRide(driver.getDriverId(), status);

        if (updated == 0) {
            if (status == VehicleStatus.AVAILABLE) {
                int recovered = driverRepository.recoverStuckOnRide(driver.getDriverId());
                if (recovered > 0) {
                    return "Driver status recovered to AVAILABLE (no active trip).";
                }
            }
            throw new RideConflictException("Cannot change status while on an active ride.");
        }

        return "Driver status updated to " + status;
    }
}