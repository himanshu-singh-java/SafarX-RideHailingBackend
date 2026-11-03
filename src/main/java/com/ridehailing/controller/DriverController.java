package com.ridehailing.controller;

import com.ridehailing.dto.DriverOnboardingRequest;
import com.ridehailing.exception.ResourceNotFoundException;
import com.ridehailing.exception.RideConflictException;
import com.ridehailing.model.Driver;
import com.ridehailing.model.VehicleStatus;
import com.ridehailing.model.Vehicles;
import com.ridehailing.repository.DriverRepository;
import com.ridehailing.repository.VehicleRepository;
import com.ridehailing.service.DriverService;
import com.ridehailing.service.JwtService;
import jakarta.persistence.Table;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    @PostMapping("/vehicle")
    public ResponseEntity<?> registerVehicle(@RequestBody DriverOnboardingRequest request, Principal principal){
        Driver driver = driverRepository.findByUser_Email(principal.getName()).
                orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        if(driver.getVehicle() != null){
            throw new RideConflictException("Vehicle is already registered for this driver.");
        }

        Vehicles vehicle = driverService.buildVehicle(request, driver);
        driver.setVehicle(vehicle);

        vehicleRepository.save(vehicle);

        driverRepository.updateStatusIfNotOnRide(driver.getDriverId(), VehicleStatus.AVAILABLE);

        return ResponseEntity.ok(Map.of("message", "Vehicle registered successfully and driver is now AVAILABLE!"));
    }

    @Transactional
    @PatchMapping("/status")
    public ResponseEntity<?> updateStatus(@RequestParam VehicleStatus status, Principal principal){
        if(status ==  VehicleStatus.ON_RIDE){
            throw new IllegalArgumentException("Cannot manually set status to ON_RIDE.");
        }

        Driver driver = driverRepository.findByUser_Email(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        int updated = driverRepository.updateStatusIfNotOnRide(driver.getDriverId(), status);

        if(updated == 0){
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot change status while on an active ride."));
        }

        return ResponseEntity.ok(Map.of("message", "Driver status updated to " + status));
    }
}
