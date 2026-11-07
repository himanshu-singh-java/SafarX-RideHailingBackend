package com.ridehailing.controller;

import com.ridehailing.dto.LoginRequest;
import com.ridehailing.dto.ProfileCompletionRequest;
import com.ridehailing.dto.UserRegistrationRequest;
import com.ridehailing.exception.UnauthorizedException;
import com.ridehailing.model.Users;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ridehailing.service.UserService;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationRequest request){

        Users savedUser = userService.register(request);

        return ResponseEntity.ok(Map.of(
                "message", "User registered successfully!",
                "email", savedUser.getEmail()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest){

        Users user = new Users();
        user.setEmail(loginRequest.getEmail());
        user.setPassword(loginRequest.getPassword());

        String token = userService.verify(user);

        if("Fail".equals(token)){
            return ResponseEntity.status(401).body("Invalid Credentials");
        }

        return ResponseEntity.ok(token);
    }

    @PostMapping("/complete-profile")
    public ResponseEntity<?> completeProfile(@RequestBody ProfileCompletionRequest request, Principal principal){

        if(principal == null){
            throw new UnauthorizedException("Please login first!");
        }

        String email = principal.getName();

        String response = userService.completeUserProfile(email, request);

        return ResponseEntity.ok(response);
    }
}
