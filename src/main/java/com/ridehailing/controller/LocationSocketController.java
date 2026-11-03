package com.ridehailing.controller;

import com.ridehailing.dto.DriverOnboardingRequest;
import com.ridehailing.dto.LocationUpdateDto;
import com.ridehailing.model.Driver;
import com.ridehailing.model.RideStatus;
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

        driverRepository.updateLocation(verifiedDriver.getDriverId(),
                                        location.getLatitude(),
                                        location.getLongitude());

        if(location.getRideId() > 0) {

            Rides newRide = rideRepository.findById(location.getRideId()).orElse(null);

            if (newRide == null || newRide.getVehicle() == null || newRide.getVehicle().getDriver() == null
                    || newRide.getVehicle().getDriver().getDriverId() != verifiedDriver.getDriverId()) {

                log.info("Unauthorized location update attempt for ride: " + location.getRideId());
                return;
            }

            if(newRide.getRideStatus() != RideStatus.IN_PROGRESS && newRide.getRideStatus() != RideStatus.ACCEPTED){
                return;
            }

            location.setDriverId(verifiedDriver.getDriverId());
            String destination = "/topic/ride/" + location.getRideId();
            messagingTemplate.convertAndSend(destination, location);
        }
    }
}
