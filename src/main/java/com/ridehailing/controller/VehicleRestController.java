package com.ridehailing.controller;

import com.ridehailing.model.Rides;
import com.ridehailing.model.Vehicles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ridehailing.repository.VehicleRepository;
import com.ridehailing.service.RideEngine;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = "http://localhost:5173")
public class VehicleRestController {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private RideEngine rideEngine;

    @GetMapping("/status/{type}")
    public ResponseEntity<?> getVehicleByType(@PathVariable String type){
        List<Vehicles> vehicles = new ArrayList<>();

        switch (type.toUpperCase()){
            case "BIKE":
                vehicles = vehicleRepository.findAllBikes();
                break;
            case "CAR":
                vehicles = vehicleRepository.findAllCars();
                break;
            case "AUTO":
                vehicles = vehicleRepository.findAllAutos();
                break;
        }

        if(vehicles == null || vehicles.isEmpty()){
            return ResponseEntity.ok(new ArrayList<>());
        }
        return ResponseEntity.ok(vehicles);
    }

    @PutMapping("/complete-ride/{vehicleId}")
    public ResponseEntity<?> completeRideByVehicle(@PathVariable int vehicleId){
        Rides completeRide = rideEngine.completeRideByVehicleId(vehicleId);
        return ResponseEntity.ok("Ride Completed");
    }
}
