package com.ridehailing.controller;
import com.ridehailing.exception.UnauthorizedException;
import com.ridehailing.mapper.RideMapper;
import com.ridehailing.dto.RideRequest;
import com.ridehailing.dto.RideResponse;
import com.ridehailing.model.RideStatus;
import com.ridehailing.model.Rides;
import org.json.HTTP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ridehailing.service.RideEngine;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rides")
@CrossOrigin(origins = "http://localhost:5173")
public class RideRestController {

    @Autowired
    private RideEngine rideEngine;

    @GetMapping("/history/{riderId}")
    public ResponseEntity<?> getAllRides(@PathVariable int riderId, Principal principal){

        if (principal == null) {
            throw new UnauthorizedException("Error: Unauthorized! Please login first.");
        }

        String riderEmail = principal.getName();
        List<Rides> rideHistory = rideEngine.getRideHistory(riderId, riderEmail);

        return ResponseEntity.ok(rideHistory);
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookRide(@RequestBody RideRequest request, Principal principal) {

        if (principal == null) {
            throw new UnauthorizedException("Error: Unauthorized! Please login first.");
        }

        String email = principal.getName();

        RideResponse response = rideEngine.bookRide(request, email);

        if("CANCELLED".equals(response.getStatus())){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping("/accept/{rideId}")
    public ResponseEntity<?> acceptRide(@PathVariable int rideId, Principal principal){
        if (principal == null) {
            throw new UnauthorizedException("Error: Unauthorized! Please login first.");
        }
        String driverEmail = principal.getName();
        RideResponse response = rideEngine.acceptRide(rideId, driverEmail);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/complete/{rideId}")
    public ResponseEntity<?> completeRide(@PathVariable int rideId, Principal principal){

        if (principal == null) {
            throw new UnauthorizedException("Error: Unauthorized! Please login first.");
        }

        String driverEmail = principal.getName();

        Rides rides = rideEngine.completeRide(rideId, driverEmail);

        RideResponse compResponse = RideMapper.mapToResponse(rides);

        return ResponseEntity.ok(compResponse);

    }

    @PutMapping("/cancel/{rideId}")
    public ResponseEntity<?> cancelRide(@PathVariable int rideId, Principal principal){
        if(principal == null){
            throw new UnauthorizedException("Error: Unauthorized! Please login first.");
        }

        String riderEmail = principal.getName();
        Rides cancelledRide = rideEngine.cancelRide(rideId, riderEmail);

        return ResponseEntity.ok(RideMapper.mapToResponse(cancelledRide));
    }

    @DeleteMapping("/delete/{rideId}")
    public ResponseEntity<?> deleteRide(@PathVariable int rideId) {

        rideEngine.deleteRide(rideId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Success! Ride with ID " + rideId + " has been permanently deleted.");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllRidesGlobal(){
        List<Rides> allRides = rideEngine.getAllGlobalRides();
        return ResponseEntity.ok(allRides);
    }

}
