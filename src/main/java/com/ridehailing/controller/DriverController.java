package com.ridehailing.controller;

import com.ridehailing.dto.DriverOnboardingRequest;
import com.ridehailing.dto.VehicleRegistrationRequest;
import com.ridehailing.exception.ResourceNotFoundException;
import com.ridehailing.exception.RideConflictException;
import com.ridehailing.model.Driver;
import com.ridehailing.model.VehicleStatus;
import com.ridehailing.model.Vehicles;
import com.ridehailing.repository.DriverRepository;
import com.ridehailing.repository.VehicleRepository;
import com.ridehailing.service.DriverService;
import com.ridehailing.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/driver")
public class DriverController {

    private final DriverService driverService;

    private final JwtService jwtService;

    private final DriverRepository driverRepository;

    private final VehicleRepository vehicleRepository;

    private final com.ridehailing.repository.RideRepository rideRepository;

    public DriverController(DriverService driverService,
                            JwtService jwtService,
                            DriverRepository driverRepository,
                            VehicleRepository vehicleRepository,
                            com.ridehailing.repository.RideRepository rideRepository){
        this.driverService = driverService;
        this.jwtService = jwtService;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.rideRepository = rideRepository;
    }

    @PostMapping("/onboard")
    public ResponseEntity<?> onBoardDriver(@Valid @RequestBody DriverOnboardingRequest request){

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
    public ResponseEntity<?> registerVehicle(@Valid @RequestBody VehicleRegistrationRequest request, Principal principal){
        Driver driver = driverRepository.findByUser_Email(principal.getName()).
                orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        if(driver.getVehicle() != null){
            throw new RideConflictException("Vehicle is already registered for this driver.");
        }

        Vehicles vehicle = driverService.buildVehicle(request.toOnboardingRequest(), driver);
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
            boolean hasActiveRide = driver.getVehicle() != null &&
                    rideRepository.findActiveRideByVehicleId(driver.getVehicle().getVehicleId()).isPresent();
            if (!hasActiveRide && status == VehicleStatus.AVAILABLE) {
                driver.setDriverStatus(VehicleStatus.AVAILABLE);
                driverRepository.save(driver);
                return ResponseEntity.ok(Map.of("message", "Driver status recovered to AVAILABLE (no active trip)."));
            }
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot change status while on an active ride."));
        }

        return ResponseEntity.ok(Map.of("message", "Driver status updated to " + status));
    }
}
