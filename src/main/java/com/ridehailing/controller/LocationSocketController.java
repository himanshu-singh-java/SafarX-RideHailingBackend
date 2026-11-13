package com.ridehailing.controller;

import com.ridehailing.dto.LocationUpdateDto;
import com.ridehailing.model.Driver;
import com.ridehailing.model.RideStatus;
import com.ridehailing.model.Rides;
import com.ridehailing.model.Vehicles;
import com.ridehailing.repository.DriverRepository;
import com.ridehailing.repository.RideRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

@Slf4j
@Controller
public class LocationSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;

    public LocationSocketController(SimpMessagingTemplate messagingTemplate,
                                    DriverRepository driverRepository,
                                    RideRepository rideRepository){
        this.messagingTemplate = messagingTemplate;
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
    }

    private boolean isValidCoordinates(double lat, double lng) {
        return !Double.isNaN(lat) && !Double.isInfinite(lat)
                && !Double.isNaN(lng) && !Double.isInfinite(lng)
                && lat >= -90.0 && lat <= 90.0
                && lng >= -180.0 && lng <= 180.0;
    }

    @Transactional
    @MessageMapping("/update-location")
    public void receiveAndForwardLocation(@Payload LocationUpdateDto location, SimpMessageHeaderAccessor headerAccessor){

        if (location == null || !isValidCoordinates(location.getLatitude(), location.getLongitude())) {
            log.warn("Invalid WebSocket coordinates received and ignored.");
            return;
        }

        Principal principal = headerAccessor.getUser();

        if(principal == null){
            log.warn("Unauthorized WebSocket Location Update Attempt Blocked!");
            return;
        }

        String driverEmail = principal.getName();
        Driver verifiedDriver = driverRepository.findByUser_Email(driverEmail).orElse(null);

        if(verifiedDriver == null){
            log.warn("Non-driver attempted to update location!");
            return;
        }

        driverRepository.updateLocation(verifiedDriver.getDriverId(),
                location.getLatitude(),
                location.getLongitude());

        if(location.getRideId() > 0) {

            Rides newRide = rideRepository.findById(location.getRideId()).orElse(null);

            if (newRide == null || newRide.getVehicle() == null || newRide.getVehicle().getDriver() == null
                    || newRide.getVehicle().getDriver().getDriverId() != verifiedDriver.getDriverId()) {

                log.info("Unauthorized location update attempt for ride: {}", location.getRideId());
                return;
            }

            if(newRide.getRideStatus() != RideStatus.IN_PROGRESS && newRide.getRideStatus() != RideStatus.ACCEPTED){
                return;
            }

            location.setDriverId(verifiedDriver.getDriverId());
            String dName = null;
            if (verifiedDriver.getUser() != null && verifiedDriver.getUser().getName() != null && !verifiedDriver.getUser().getName().isBlank()) {
                dName = verifiedDriver.getUser().getName().trim();
                if (!dName.equals(verifiedDriver.getDriverName())) {
                    verifiedDriver.setDriverName(dName);
                    driverRepository.save(verifiedDriver);
                }
            } else if (verifiedDriver.getDriverName() != null && !verifiedDriver.getDriverName().isBlank()) {
                dName = verifiedDriver.getDriverName().trim();
            } else if (verifiedDriver.getUser() != null && verifiedDriver.getUser().getEmail() != null) {
                dName = verifiedDriver.getUser().getEmail().split("@")[0];
            }
            location.setDriverName(dName);
            location.setRating(verifiedDriver.getRating());

            Vehicles v = newRide.getVehicle();
            if (v != null) {
                location.setVehicleNumber(v.getPlateNumber());
                if (v.getVehicleCategory() != null) {
                    location.setVehicleType(v.getVehicleCategory().name());
                }
                location.setVehicleModel(v.getVehicleModelName());
            }

            String destination = "/topic/ride/" + location.getRideId();
            messagingTemplate.convertAndSend(destination, location);
        }
    }
}