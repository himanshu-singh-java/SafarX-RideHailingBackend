package com.ridehailing.controller;

import com.ridehailing.dto.DriverOnboardingRequest;
import com.ridehailing.service.DriverService;
import com.ridehailing.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/driver")
public class DriverController {

    @Autowired
    private DriverService driverService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/onboard")
    public ResponseEntity<?> onBoardDriver(@RequestBody DriverOnboardingRequest request){

        String message = driverService.onBoardDriver(request);
        String token = jwtService.generateToken(request.getEmail(), "ROLE_DRIVER");

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", message,
                "token", token,
                "role", "ROLE_DRIVER",
                "email", request.getEmail()
        ));
    }
}
