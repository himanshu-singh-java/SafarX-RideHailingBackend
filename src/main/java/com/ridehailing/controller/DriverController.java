package com.ridehailing.controller;

import com.ridehailing.dto.DriverLocationUpdateRequest;
import com.ridehailing.dto.DriverOnboardingRequest;
import com.ridehailing.dto.VehicleRegistrationRequest;
import com.ridehailing.exception.UnauthorizedException;
import com.ridehailing.model.Driver;
import com.ridehailing.model.VehicleStatus;
import com.ridehailing.model.Vehicles;
import com.ridehailing.service.DriverService;
import com.ridehailing.service.JwtService;
import com.ridehailing.service.OtpService;
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
    private final OtpService otpService;

    public DriverController(DriverService driverService,
                            JwtService jwtService,
                            OtpService otpService){
        this.driverService = driverService;
        this.jwtService = jwtService;
        this.otpService = otpService;
    }

    @PostMapping("/onboard")
    public ResponseEntity<?> onBoardDriver(@Valid @RequestBody DriverOnboardingRequest request){

        boolean isOtpValid = otpService.verifyOtp(request.getEmail(), request.getOtp());

        if(!isOtpValid){
            throw new UnauthorizedException("Invalid or expired OTP!");
        }

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

    @Transactional
    @PutMapping("/location")
    public ResponseEntity<?> updateLocation(@Valid @RequestBody DriverLocationUpdateRequest request, Principal principal){
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (request == null || request.getLatitude() == null || request.getLongitude() == null) {
            throw new IllegalArgumentException("Latitude and longitude are required.");
        }
        double lat = request.getLatitude();
        double lng = request.getLongitude();
        if (Double.isNaN(lat) || Double.isInfinite(lat) || Double.isNaN(lng) || Double.isInfinite(lng)
                || lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) {
            throw new IllegalArgumentException("Invalid coordinates: Latitude must be between -90 and 90, and longitude between -180 and 180.");
        }
        if (Math.abs(lat) < 1e-6 && Math.abs(lng) < 1e-6) {
            throw new IllegalArgumentException("Coordinates (0.0, 0.0) are invalid.");
        }

        driverService.updateDriverLocation(principal.getName(), lat, lng);

        return ResponseEntity.ok(Map.of(
                "message", "Driver location updated successfully",
                "latitude", lat,
                "longitude", lng
        ));
    }
}
