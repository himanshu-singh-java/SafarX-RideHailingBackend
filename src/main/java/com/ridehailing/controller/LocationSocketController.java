package com.ridehailing.controller;

import com.ridehailing.dto.LocationUpdateDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class LocationSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/update-location")
    public void receiveAndForwardLocation(@Payload LocationUpdateDto location){

        System.out.println("Driver " + location.getDriverId() + " is at: " + location.getLatitude());

        String destination = "/topic/ride/" + location.getRideId();

        messagingTemplate.convertAndSend(destination, location);
    }
}
