package com.ridehailing.controller;

import com.ridehailing.dto.ProfileCompletionRequest;
import com.ridehailing.exception.UnauthorizedException;
import com.ridehailing.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ridehailing.service.UserService;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Users> register(@RequestBody Users users){

        Users savedUser = userService.register(users);
        return ResponseEntity.ok(savedUser);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Users user){
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
