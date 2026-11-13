package com.ridehailing.controller;

import com.ridehailing.dto.RideRequest;
import com.ridehailing.dto.RideResponse;
import com.ridehailing.exception.UnauthorizedException;
import com.ridehailing.service.RideEngine;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rides")
public class RideRestController {

    private final RideEngine rideEngine;

    public RideRestController(RideEngine rideEngine) {
        this.rideEngine = rideEngine;
    }

    @GetMapping("/history/{riderId}")
    public ResponseEntity<List<RideResponse>> getAllRides(@PathVariable int riderId, Principal principal){
        if (principal == null) {
            throw new UnauthorizedException("Error: Unauthorized! Please login first.");
        }
        return ResponseEntity.ok(rideEngine.getRideHistory(riderId, principal.getName()));
    }

    @PostMapping("/book")
    public ResponseEntity<RideResponse> bookRide(@Valid @RequestBody RideRequest request, Principal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Error: Unauthorized! Please login first.");
        }

        RideResponse response = rideEngine.bookRide(request, principal.getName());

        if("CANCELLED".equals(response.getStatus())){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping("/accept/{rideId}")
    public ResponseEntity<RideResponse> acceptRide(@PathVariable int rideId, Principal principal){
        if (principal == null) {
            throw new UnauthorizedException("Error: Unauthorized! Please login first.");
        }
        return ResponseEntity.ok(rideEngine.acceptRide(rideId, principal.getName()));
    }

    @PutMapping("/start/{rideId}")
    public ResponseEntity<RideResponse> startRide(@PathVariable int rideId, Principal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Error: Unauthorized! Please login first.");
        }
        return ResponseEntity.ok(rideEngine.startRide(rideId, principal.getName()));
    }

    @PutMapping("/complete/{rideId}")
    public ResponseEntity<RideResponse> completeRide(@PathVariable int rideId, Principal principal){
        if (principal == null) {
            throw new UnauthorizedException("Error: Unauthorized! Please login first.");
        }
        return ResponseEntity.ok(rideEngine.completeRide(rideId, principal.getName()));
    }

    @PutMapping("/cancel/{rideId}")
    public ResponseEntity<RideResponse> cancelRide(@PathVariable int rideId, Principal principal){
        if(principal == null){
            throw new UnauthorizedException("Error: Unauthorized! Please login first.");
        }
        return ResponseEntity.ok(rideEngine.cancelRide(rideId, principal.getName()));
    }

    @DeleteMapping("/delete/{rideId}")
    public ResponseEntity<?> deleteRide(@PathVariable int rideId) {
        rideEngine.deleteRide(rideId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Success! Ride with ID " + rideId + " has been permanently deleted.");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<RideResponse>> getAllRidesGlobal(){
        return ResponseEntity.ok(rideEngine.getAllGlobalRides());
    }
}