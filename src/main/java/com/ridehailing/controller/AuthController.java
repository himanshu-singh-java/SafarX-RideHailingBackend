package com.ridehailing.controller;

import com.ridehailing.dto.UserProfileResponse;
import com.ridehailing.exception.UnauthorizedException;
import com.ridehailing.model.Auto;
import com.ridehailing.model.Bike;
import com.ridehailing.model.Car;
import com.ridehailing.model.Driver;
import com.ridehailing.model.Riders;
import com.ridehailing.model.Role;
import com.ridehailing.model.Users;
import com.ridehailing.model.Vehicles;
import com.ridehailing.repository.DriverRepository;
import com.ridehailing.repository.RiderRepository;
import com.ridehailing.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final RiderRepository riderRepository;

    public AuthController(UserRepository userRepository,
                          DriverRepository driverRepository,
                          RiderRepository riderRepository){
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.riderRepository = riderRepository;
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<UserProfileResponse> getCurrentUser(Principal principal){
        if(principal == null || principal.getName() == null){
            throw new UnauthorizedException("Authentication required to fetch user profile");
        }

        String email = principal.getName();
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User account no longer exists"));

        Role role = user.getRole();
        String roleName = role != null ? role.name() : "ROLE_RIDER";

        if(role == Role.ROLE_RIDER){
            Riders rider = riderRepository.findByUsers_Email(email).orElse(null);
            String riderName = (rider != null && rider.getRiderName() != null && !rider.getRiderName().isBlank())
                    ? rider.getRiderName()
                    : user.getName();

            return ResponseEntity.ok(new UserProfileResponse(
                    user.getEmail(),
                    roleName,
                    riderName,
                    rider != null ? rider.getRiderID() : null,
                    null,
                    null
            ));
        }

        if (role == Role.ROLE_DRIVER) {
            Driver driver = driverRepository.findByUser_Email(email).orElse(null);
            String driverName = (driver != null && driver.getDriverName() != null && !driver.getDriverName().isBlank())
                    ? driver.getDriverName()
                    : user.getName();

            String vehicleType = null;
            String vehicleModel = null;
            String plateNumber = null;
            Double rating = driver != null ? driver.getRating() : 5.0;

            if (driver != null && driver.getVehicle() != null) {
                Vehicles v = driver.getVehicle();
                plateNumber = v.getPlateNumber();
                if (v instanceof Car car) {
                    vehicleType = "CAR";
                    vehicleModel = car.getCarModel();
                } else if (v instanceof Bike bike) {
                    vehicleType = "BIKE";
                    vehicleModel = bike.getBikeModel();
                } else if (v instanceof Auto auto) {
                    vehicleType = "AUTO";
                    vehicleModel = auto.getAutoModel();
                }
            }

            return ResponseEntity.ok(new UserProfileResponse(
                    user.getEmail(),
                    roleName,
                    driverName,
                    null,
                    driver != null ? driver.getDriverId() : null,
                    driver != null && driver.getDriverStatus() != null ? driver.getDriverStatus().name() : null,
                    vehicleType,
                    vehicleModel,
                    plateNumber,
                    rating
            ));
        }

        return ResponseEntity.ok(new UserProfileResponse(
                user.getEmail(),
                roleName,
                user.getName(),
                null,
                null,
                null
        ));
    }
}
