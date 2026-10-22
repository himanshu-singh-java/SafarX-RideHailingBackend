package com.ridehailing.service;
import com.ridehailing.dto.RideResponse;
import com.ridehailing.exception.ResourceNotFoundException;
import com.ridehailing.exception.RideConflictException;
import com.ridehailing.repository.DriverRepository;
import lombok.extern.slf4j.Slf4j;
import com.ridehailing.dto.RideRequest;
import com.ridehailing.mapper.RideMapper;
import com.ridehailing.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ridehailing.repository.RideRepository;
import com.ridehailing.repository.RiderRepository;
import com.ridehailing.repository.VehicleRepository;
import com.ridehailing.util.LocationUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
public class RideEngine {

    private VehicleRepository vehicleRepository;
    private RideRepository rideRepository;
    private RiderRepository riderRepository;
    private DriverRepository driverRepository;
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    public RideEngine(VehicleRepository vehicleRepository,
                      RideRepository rideRepository,
                      RiderRepository riderRepository,
                      DriverRepository driverRepository,
                      SimpMessagingTemplate messagingTemplate){
        this.vehicleRepository = vehicleRepository;
        this.rideRepository = rideRepository;
        this.riderRepository = riderRepository;
        this.driverRepository = driverRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public RideResponse bookRide(RideRequest request, String email){

        Riders rider = riderRepository.findWithLockByUsers_Email(email)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Rider profile not found for this user!"));

        boolean hasActiveRide = rideRepository.existsByRider_RiderIDAndRideStatusIn(
                rider.getRiderID(), List.of(RideStatus.ACCEPTED, RideStatus.IN_PROGRESS)
        );

        if (hasActiveRide) {
            throw new RideConflictException("Error: You already have an active ride! Please complete or cancel it first.");
        }

        int[] searchRadii = {2, 5, 10};
        Vehicles nearestVehicle = null;

        for(int radius : searchRadii) {
            log.info("Searching for Nearest " + request.getVehicleType());

            nearestVehicle = vehicleRepository.
                    findNearestAvailableVehicle(request.getVehicleType(),
                            request.getPickupLat(), request.getPickupLng(), radius);

            if (nearestVehicle != null) {
                log.info("Found nearest driver {} within {} km! Sending request...",
                        nearestVehicle.getDriver().getDriverName(), radius);
                break;
            }
        }

        if (nearestVehicle == null) {
            throw new ResourceNotFoundException("Error: No available " + request.getVehicleType() + " found right now.");
        }

        double estimatedDistance = LocationUtils.calculateDistance(request.getPickupLat(),
                request.getPickupLng(), request.getDropLat(), request.getDropLng());

        Double estimatedFare = nearestVehicle.calculateFare(estimatedDistance);

        Rides newRide = RideMapper.mapToEntity(request);
        newRide.setRider(rider);
        newRide.setVehicle(nearestVehicle);
        newRide.setDistanceKm(estimatedDistance);
        newRide.setFare(estimatedFare);
        newRide.setRideStatus(RideStatus.REQUESTED);
        Rides savedRide = rideRepository.save(newRide);

        RideResponse response = RideMapper.mapToResponse(savedRide);

        int targetDriverId = nearestVehicle.getDriver().getDriverId();
        messagingTemplate.convertAndSend("/topic/driver/" + targetDriverId, response);
        log.info("Notification sent to Driver ID: {} on /topic/driver/{}", targetDriverId, targetDriverId);

        return  response;
    }

    public RideResponse acceptRide(int rideId, String driverEmail){
        Rides ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Ride with ID " + rideId + " not found!"));

        Driver loggedInDriver = driverRepository.findByUser_Email(driverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Driver profile not found!"));

        if(ride.getVehicle() == null || ride.getVehicle().getDriver() == null
        || ride.getVehicle().getDriver().getDriverId() != loggedInDriver.getDriverId()){
            throw new RideConflictException("Error: Unauthorized! You are not assigned to this ride.");
        }

        if(ride.getRideStatus() != RideStatus.REQUESTED){
            throw new RideConflictException("This ride is no longer available to accept.");
        }

        int claimed = driverRepository.claimDriver(
                loggedInDriver.getDriverId(),
                VehicleStatus.ON_RIDE,
                VehicleStatus.AVAILABLE
        );

        if(claimed != 1){
            throw new RideConflictException("Driver is not available or already on another ride.");
        }

        ride.setRideStatus(RideStatus.ACCEPTED);
        ride.setStartTime(LocalDateTime.now());

        Rides updatedRide = rideRepository.save(ride);

        RideResponse response = RideMapper.mapToResponse(updatedRide);

        messagingTemplate.convertAndSend("/topic/ride/" + rideId, response);
        log.info("Ride {} ACCEPTED by Driver {}. Notified Rider on /topic/ride/{}", rideId, loggedInDriver, rideId);

        return  response;
    }

    public Rides completeRide(int rideId){

        Rides ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Error: Ride with ID " + rideId + " not found!"));

        if(ride.getRideStatus() == RideStatus.COMPLETED){
            throw new RuntimeException("This ride is already marked as COMPLETED.");
        }

        ride.setRideStatus(RideStatus.COMPLETED);
        ride.setEndTime(LocalDateTime.now());


        Vehicles vehicles = ride.getVehicle();
        if (vehicles != null && vehicles.getDriver() != null) {
            Driver driver = vehicles.getDriver();
            driver.setDriverStatus(VehicleStatus.AVAILABLE);
            driver.setCurrentLat(ride.getDropLat());
            driver.setCurrentLng(ride.getDropLng());
            vehicleRepository.save(vehicles);
        }

        Rides savedRide = rideRepository.save(ride);

        RideResponse compResponse = RideMapper.mapToResponse(savedRide);

        messagingTemplate.convertAndSend("/topic/ride/" + rideId, compResponse);

        return savedRide;
    }

    public List<Rides> getRideHistory(int riderId){

        Riders rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new RuntimeException("Error: Rider with ID " + riderId + " not found!"));

        return  rideRepository.findByRider_RiderIDOrderByRideIdDesc(riderId);
    }

    public void deleteRide(int rideId) {

        Rides rides = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Error: Cannot delete. Ride with ID " + rideId + " not found!"));

        if(rides.getRideStatus() == RideStatus.ACCEPTED){
            Vehicles vehicles = rides.getVehicle();

            if(vehicles != null && vehicles.getDriver() != null){
                vehicles.getDriver().setDriverStatus(VehicleStatus.AVAILABLE);
                vehicleRepository.save(vehicles);
            }
        }

        rideRepository.deleteById(rideId);
    }

    public List<Rides> getAllGlobalRides() {
        return rideRepository.findAllByOrderByRideIdDesc();
    }

    public Rides completeRideByVehicleId(int vehicleId){

        Rides activeRide = rideRepository.findActiveRideByVehicleId(vehicleId)
                .orElseThrow(() -> new RuntimeException("No Active Ride found for Vehicle Id :" + vehicleId));

        activeRide.setRideStatus(RideStatus.COMPLETED);
        activeRide.setEndTime(LocalDateTime.now());

        Vehicles vehicles = activeRide.getVehicle();
        if(vehicles != null && vehicles.getDriver() != null){
            vehicles.getDriver().setDriverStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicles);
        }
        return rideRepository.save(activeRide);
    }
}
