package com.ridehailing.service;

import com.ridehailing.model.Driver;
import com.ridehailing.model.Rides;
import com.ridehailing.repository.DriverRepository;
import com.ridehailing.repository.RideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebSocketAuthService {

    private final DriverRepository driverRepository;

    private final RideRepository rideRepository;

    public WebSocketAuthService(DriverRepository driverRepository, RideRepository rideRepository){
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
    }

    @Transactional(readOnly = true)
    public boolean canSubscribeToDriver(String email, String requestedDriverId){
        Driver driver = driverRepository.findByUser_Email(email).orElse(null);
        return driver != null && String.valueOf(driver.getDriverId()).equals(requestedDriverId);
    }

    @Transactional(readOnly = true)
    public boolean canSubscribeToRide(String email, String requestedRideId){
        try {
            Rides ride = rideRepository.findById(Integer.parseInt(requestedRideId)).orElse(null);
            if(ride == null){
                return  false;
            }

            if (ride.getRider().getUsers().getEmail().equals(email)){
                return  true;
            }

            if(ride.getVehicle() != null && ride.getVehicle().getDriver() != null &&
            ride.getVehicle().getDriver().getUser().getEmail().equals(email)){
                return true;
            }
            return false;
        }
        catch (NumberFormatException e){
            return false;
        }
    }
}
