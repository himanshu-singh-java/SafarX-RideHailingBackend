package com.ridehailing.controller;

import com.ridehailing.dto.DriverOnboardingRequest;
import com.ridehailing.exception.ResourceNotFoundException;
import com.ridehailing.model.Driver;
import com.ridehailing.model.VehicleStatus;
import com.ridehailing.model.Vehicles;
import com.ridehailing.repository.DriverRepository;
import com.ridehailing.repository.VehicleRepository;
import com.ridehailing.service.DriverService;
import com.ridehailing.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/driver")
public class DriverController {

    @Autowired
    private DriverService driverService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @PostMapping("/onboard")
    public ResponseEntity<?> onBoardDriver(@RequestBody DriverOnboardingRequest request){

        Driver driver = driverService.onBoardDriver(request);
        String token = jwtService.generateToken(request.getEmail(), "ROLE_DRIVER");

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Driver & Vehicle Onboarded Successfully!",
                "token", token,
                "role", "ROLE_DRIVER",
                "email", request.getEmail(),
                "driverId", driver.getDriverId()
        ));
    }

    @PostMapping("/vehicle")
    public ResponseEntity<?> registerVehicle(@RequestBody DriverOnboardingRequest request, Principal principal){
        Driver driver = driverRepository.findByUser_Email(principal.getName()).
                orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        Vehicles vehicle;
        String type = request.getVehicleType().toUpperCase();
        switch (type){
            case "CAR":
                vehicle = new com.ridehailing.model.Car();
                break;
            case "BIKE":
                vehicle = new com.ridehailing.model.Bike();
                break;
            case "AUTO":
                vehicle = new com.ridehailing.model.Auto();
                break;
            default:
                throw new IllegalArgumentException("Invalid vehicle type");
        }

        vehicle.setPlateNumber(request.getPlateNumber());
        vehicle.setDriver(driver);
        vehicleRepository.save(vehicle);

        driver.setDriverStatus(VehicleStatus.AVAILABLE);
        driverRepository.save(driver);

        return ResponseEntity.ok(Map.of("message", "Vehicle registered successfully and driver is now AVAILABLE!"));
    }

    @PatchMapping("/status")
    public ResponseEntity<?> updateStatus(@RequestParam VehicleStatus status, Principal principal){
        Driver driver = driverRepository.findByUser_Email(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        if(driver.getDriverStatus() == VehicleStatus.ON_RIDE){
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot change status while on an active ride."));
        }

        driver.setDriverStatus(status);
        driverRepository.save(driver);
        return ResponseEntity.ok(Map.of("message", "Driver status updated to " + status));
    }
}
