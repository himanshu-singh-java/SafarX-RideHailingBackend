package com.ridehailing.service;

import com.ridehailing.model.RideStatus;
import com.ridehailing.model.Rides;
import com.ridehailing.repository.RideRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class RideBroadcastScheduler {

    private final RideRepository rideRepository;

    private final RideEngine rideEngine;

    public RideBroadcastScheduler(RideRepository rideRepository, RideEngine rideEngine){
        this.rideRepository = rideRepository;
        this.rideEngine = rideEngine;
    }

    @Scheduled(fixedDelay = 5000)
    public void processTimedOutRides(){
        LocalDateTime cutOffTime = LocalDateTime.now().minusSeconds(20);

        List<Rides> timedOutRides = rideRepository.findByRideStatusAndLastBroadcastAtBefore(
                RideStatus.REQUESTED, cutOffTime
        );

        for(Rides ride : timedOutRides){
            try{
                rideEngine.handleRideTimeout(ride.getRideId());
            }
            catch (Exception e){
                log.error("Error processing timeout for ride {}: {}", ride.getRideId(), e.getMessage(), e);
            }
        }
    }

}
