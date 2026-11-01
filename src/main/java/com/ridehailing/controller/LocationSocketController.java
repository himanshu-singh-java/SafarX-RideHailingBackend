package com.ridehailing.controller;

import com.ridehailing.dto.DriverOnboardingRequest;
import com.ridehailing.dto.LocationUpdateDto;
import com.ridehailing.model.Driver;
import com.ridehailing.model.Rides;
import com.ridehailing.repository.DriverRepository;
import com.ridehailing.repository.RideRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
public class LocationSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private RideRepository rideRepository;

    @MessageMapping("/update-location")
    public void receiveAndForwardLocation(@Payload LocationUpdateDto location, SimpMessageHeaderAccessor headerAccessor){

        Principal principal = headerAccessor.getUser();

        if(principal == null){
            System.out.println("Unauthorized WebSocket Location Update Attempt Blocked!");
            return;
        }

        String driverEmail = principal.getName();
        Driver verifiedDriver = driverRepository.findByUser_Email(driverEmail).orElse(null);

        if(verifiedDriver == null){
            System.out.println("Non-driver attempted to update location!");
            return;
        }

        Rides ride = rideRepository.findById(location.getRideId()).orElse(null);
        if(ride == null || ride.getVehicle() == null || ride.getVehicle().getDriver() == null
        || ride.getVehicle().getDriver().getDriverId() != verifiedDriver.getDriverId()){
            log.info("Unauthorized location update attempt for ride: " + location.getRideId());
            return;
        }

        driverRepository.updateLocation(verifiedDriver.getDriverId(),
                                        location.getLatitude(),
                                        location.getLongitude());

        location.setDriverId(verifiedDriver.getDriverId());

        System.out.println("Driver " + location.getDriverId() + " is at: " + location.getLatitude());

        String destination = "/topic/ride/" + location.getRideId();

        messagingTemplate.convertAndSend(destination, location);
    }
}
