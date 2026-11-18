package com.ridehailing.controller;

import com.ridehailing.dto.LoginRequest;
import com.ridehailing.dto.ProfileCompletionRequest;
import com.ridehailing.dto.SendOtpRequest;
import com.ridehailing.dto.UserRegistrationRequest;
import com.ridehailing.exception.UnauthorizedException;
import com.ridehailing.model.Users;
import com.ridehailing.service.OtpService;
import com.ridehailing.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;
    private final OtpService otpService;

    public UserController(UserService userService, OtpService otpService){
        this.userService = userService;
        this.otpService = otpService;
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody SendOtpRequest request){
        otpService.generateAndSendOtp(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully to " + request.getEmail()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationRequest request){

        boolean isOtpValid = otpService.verifyOtp(request.getEmail(), request.getOtp());

        if(!isOtpValid){
            throw new UnauthorizedException("Invalid or expired OTP!");
        }

        Users savedUser = userService.register(request);

        return ResponseEntity.ok(Map.of(
                "message", "User registered successfully!",
                "email", savedUser.getEmail()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest loginRequest){
        Users user = new Users();
        user.setEmail(loginRequest.getEmail());
        user.setPassword(loginRequest.getPassword());

        String token = userService.verify(user);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/complete-profile")
    public ResponseEntity<?> completeProfile(@Valid @RequestBody ProfileCompletionRequest request, Principal principal){
        if(principal == null){
            throw new UnauthorizedException("Please login first!");
        }

        String response = userService.completeUserProfile(principal.getName(), request);
        return ResponseEntity.ok(response);
    }
}