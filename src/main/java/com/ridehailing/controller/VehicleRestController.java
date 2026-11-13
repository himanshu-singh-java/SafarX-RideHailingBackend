package com.ridehailing.controller;

import com.ridehailing.dto.VehicleResponse;
import com.ridehailing.model.Vehicles;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ridehailing.repository.VehicleRepository;
import com.ridehailing.service.RideEngine;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleRestController {

    private final VehicleRepository vehicleRepository;

    private final RideEngine rideEngine;

    public VehicleRestController(VehicleRepository vehicleRepository, RideEngine rideEngine){
        this.vehicleRepository = vehicleRepository;
        this.rideEngine = rideEngine;
    }

    @GetMapping("/status/{type}")
    public ResponseEntity<List<VehicleResponse>> getVehicleByType(@PathVariable String type){
        List<Vehicles> vehicles = switch (type.toUpperCase()){
            case "BIKE" -> vehicleRepository.findAllBikes();
            case "CAR" -> vehicleRepository.findAllCars();
            case "AUTO" -> vehicleRepository.findAllAutos();
            default -> new ArrayList<>();
        };

        if(vehicles == null || vehicles.isEmpty()){
            return ResponseEntity.ok(new ArrayList<>());
        }

        List<VehicleResponse> response = vehicles.stream()
                .map(VehicleResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/complete-ride/{vehicleId}")
    public ResponseEntity<?> completeRideByVehicle(@PathVariable int vehicleId){
        rideEngine.completeRideByVehicleId(vehicleId);
        return ResponseEntity.ok("Ride Completed");
    }
}
