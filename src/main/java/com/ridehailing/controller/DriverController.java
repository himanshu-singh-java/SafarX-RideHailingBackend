package com.ridehailing.controller;

import com.ridehailing.dto.DriverOnboardingRequest;
import com.ridehailing.dto.VehicleRegistrationRequest;
import com.ridehailing.model.Driver;
import com.ridehailing.model.VehicleStatus;
import com.ridehailing.model.Vehicles;
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

    public DriverController(DriverService driverService,
                            JwtService jwtService){
        this.driverService = driverService;
        this.jwtService = jwtService;
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
        driverService.registerVehicleForDriver(principal.getName(), request);
        return ResponseEntity.ok(Map.of("message", "Vehicle registered successfully and driver is now AVAILABLE!"));
    }

    @Transactional
    @PutMapping("/vehicle")
    public ResponseEntity<?> updateVehicle(@Valid @RequestBody VehicleRegistrationRequest request, Principal principal){
        Vehicles updatedVehicle = driverService.updateDriverVehicle(principal.getName(), request);

        return ResponseEntity.ok(Map.of(
                "message", "Vehicle details updated successfully!",
                "vehicleType", updatedVehicle.getVehicleCategory().name(),
                "model", updatedVehicle.getVehicleModelName(),
                "plateNumber", updatedVehicle.getPlateNumber()
        ));
    }

    @Transactional
    @PatchMapping("/status")
    public ResponseEntity<?> updateStatus(@RequestParam VehicleStatus status, Principal principal){
        String message = driverService.updateDriverStatus(principal.getName(), status);
        return ResponseEntity.ok(Map.of("message", message));
    }
}
