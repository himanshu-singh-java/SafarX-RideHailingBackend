package com.ridehailing.controller;

import com.ridehailing.dto.DriverOnboardingRequest;
import com.ridehailing.dto.LocationUpdateDto;
import com.ridehailing.model.Driver;
import com.ridehailing.repository.DriverRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class LocationSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private DriverRepository driverRepository;

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

        verifiedDriver.setCurrentLat(location.getLatitude());
        verifiedDriver.setCurrentLng(location.getLongitude());
        driverRepository.save(verifiedDriver);

        location.setDriverId(verifiedDriver.getDriverId());

        System.out.println("Driver " + location.getDriverId() + " is at: " + location.getLatitude());

        String destination = "/topic/ride/" + location.getRideId();

        messagingTemplate.convertAndSend(destination, location);
    }
}
