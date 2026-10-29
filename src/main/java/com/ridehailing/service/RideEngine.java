package com.ridehailing.service;
import com.ridehailing.dto.RideResponse;
import com.ridehailing.exception.ResourceNotFoundException;
import com.ridehailing.exception.RideConflictException;
import com.ridehailing.repository.*;
import lombok.extern.slf4j.Slf4j;
import com.ridehailing.dto.RideRequest;
import com.ridehailing.mapper.RideMapper;
import com.ridehailing.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ridehailing.util.LocationUtils;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class RideEngine {

    private VehicleRepository vehicleRepository;
    private RideRepository rideRepository;
    private RiderRepository riderRepository;
    private DriverRepository driverRepository;
    private SimpMessagingTemplate messagingTemplate;
    private RideNotifiedDriverRepository rideNotifiedDriverRepository;

    private final int[] SEARCH_RADII = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    @Autowired
    public RideEngine(VehicleRepository vehicleRepository,
                      RideRepository rideRepository,
                      RiderRepository riderRepository,
                      DriverRepository driverRepository,
                      SimpMessagingTemplate messagingTemplate,
                      RideNotifiedDriverRepository rideNotifiedDriverRepository){

        this.vehicleRepository = vehicleRepository;
        this.rideRepository = rideRepository;
        this.riderRepository = riderRepository;
        this.driverRepository = driverRepository;
        this.messagingTemplate = messagingTemplate;
        this.rideNotifiedDriverRepository = rideNotifiedDriverRepository;
    }

    public RideResponse bookRide(RideRequest request, String email){

        Riders rider = riderRepository.findWithLockByUsers_Email(email)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Rider profile not found for this user!"));

        boolean hasActiveRide = rideRepository.existsByRider_RiderIDAndRideStatusIn(
                rider.getRiderID(), List.of(RideStatus.REQUESTED, RideStatus.ACCEPTED, RideStatus.IN_PROGRESS)
        );

        if (hasActiveRide) {
            throw new RideConflictException("Error: You already have an active ride! Please complete or cancel it first.");
        }

        double estimatedDistance = LocationUtils.calculateDistance(request.getPickupLat(),
                request.getPickupLng(), request.getDropLat(), request.getDropLng());


        Rides newRide = RideMapper.mapToEntity(request);
        newRide.setRider(rider);
        newRide.setVehicle(null);
        newRide.setDistanceKm(estimatedDistance);
        newRide.setFare(0.0);
        newRide.setRideStatus(RideStatus.REQUESTED);

        newRide.setVehicleType(request.getVehicleType());
        newRide.setCurrentRadiusIndex(0);
        newRide.setLastBroadcastAt(LocalDateTime.now());

        Rides savedRide = rideRepository.save(newRide);

        broadcastRide(savedRide.getRideId());

        return RideMapper.mapToResponse(savedRide);
    }

    @Transactional
    public void broadcastRide(int rideId){

        Rides ride = rideRepository.findById(rideId).orElse(null);

        if(ride == null || ride.getRideStatus() != RideStatus.REQUESTED){
            return;
        }

        int radiusIndex = ride.getCurrentRadiusIndex();

        while (radiusIndex < SEARCH_RADII.length) {
            int currentRadius = SEARCH_RADII[radiusIndex];

            List<Vehicles> availableVehicles = vehicleRepository.findAllAvailableVehiclesForRide(
                    ride.getVehicleType(), ride.getPickupLat(), ride.getPickupLng(), currentRadius, rideId
            );

            if (availableVehicles.isEmpty()) {
                radiusIndex++;
                continue;
            }

            double fareToSave = (ride.getFare() == null || ride.getFare() == 0.0) ?
                    availableVehicles.get(0).calculateFare(ride.getDistanceKm()) : ride.getFare();

            int updatedRows = rideRepository.updateBroadcastStateAtDB(
                    rideId,
                    fareToSave,
                    radiusIndex,
                    LocalDateTime.now()
            );

            if(updatedRows == 0){
                log.info("Ride {} was accepted during broadcast execution. Aborting further broadcast to prevent stale write.", rideId);
                return;
            }

            ride = rideRepository.findById(rideId).orElseThrow();

            RideResponse response = RideMapper.mapToResponse(ride);

            for (Vehicles vehicles : availableVehicles) {
                int driverId = vehicles.getDriver().getDriverId();
                messagingTemplate.convertAndSend("/topic/driver/" + driverId, response);
                rideNotifiedDriverRepository.save(new RideNotifiedDriver(rideId, driverId));
            }

            log.info("Ride {} broadcasted to {} drivers in {} km radius.", rideId, availableVehicles.size(), currentRadius);
            return;
        }

        log.info("No drivers accepted in max radius for ride {}", rideId);
        cancelRide(rideId);
    }

    @Transactional
    public void handleRideTimeout(int rideId){
        Rides ride = rideRepository.findById(rideId).orElse(null);

        if(ride == null || ride.getRideStatus() != RideStatus.REQUESTED){
            return;
        }

        log.info("Timeout for ride {} at radius index {}. Expanding radius.", rideId, ride.getCurrentRadiusIndex());

        List<RideNotifiedDriver> notifiedDrivers = rideNotifiedDriverRepository.findByRideId(rideId);

        Map<String, Object> timeoutEvent = new HashMap<>();
        timeoutEvent.put("type", "TIMEOUT");
        timeoutEvent.put("rideId", rideId);

        for(RideNotifiedDriver skipped : notifiedDrivers){
            messagingTemplate.convertAndSend("/topic/driver/" + skipped.getDriverId(), timeoutEvent);
        }

        ride.setCurrentRadiusIndex(ride.getCurrentRadiusIndex() + 1);
        rideRepository.save(ride);

        broadcastRide(rideId);
    }

    @Transactional
    public Rides cancelRide(int rideId, String riderEmail){

        Rides ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found!"));

        if(riderEmail != null && !ride.getRider().getUsers().getEmail().equals(riderEmail)){
            throw new RideConflictException("Error: Unauthorized! You can only cancel your own rides.");
        }

        if(ride.getRideStatus() == RideStatus.COMPLETED || ride.getRideStatus() == RideStatus.CANCELLED){
            throw new RideConflictException("Error: Ride is already " + ride.getRideStatus() + " and cannot be cancelled.");
        }

        if((ride.getRideStatus() == RideStatus.ACCEPTED || ride.getRideStatus() == RideStatus.IN_PROGRESS)
        && ride.getVehicle() != null && ride.getVehicle().getDriver() != null){
            Driver assignedDriver = ride.getVehicle().getDriver();
            assignedDriver.setDriverStatus(VehicleStatus.AVAILABLE);
        }

        ride.setRideStatus(RideStatus.CANCELLED);
        Rides cancelledRide = rideRepository.save(ride);

        messagingTemplate.convertAndSend("/topic/ride/" + rideId, RideMapper.mapToResponse(cancelledRide));
        return cancelledRide;
    }

    @Transactional
    public void cancelRide(int rideId){
        cancelRide(rideId, null);
    }


    @Transactional
    public RideResponse acceptRide(int rideId, String driverEmail){

        Driver loggedInDriver = driverRepository.findByUser_Email(driverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Driver profile not found!"));

        Vehicles acceptingVehicle = vehicleRepository.findByDriver_DriverId(loggedInDriver.getDriverId());

        if(acceptingVehicle == null){
            throw new RideConflictException("Error: You do not have a registered vehicle to accept this ride.");
        }

        boolean wasNotified = rideNotifiedDriverRepository.existsByRideIdAndDriverId(rideId, loggedInDriver.getDriverId());

        if(!wasNotified){
            throw new RideConflictException("Error: Unauthorized! You were not notified for this ride or your vehicle type doesn't match.");
        }

        Rides rideForDistance = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found!"));

        double finalFare = acceptingVehicle.calculateFare(rideForDistance.getDistanceKm());

        int rideClaimed = rideRepository.claimRideAtDB(
                rideId,
                acceptingVehicle,
                finalFare,
                RideStatus.ACCEPTED,
                RideStatus.REQUESTED
        );

        if(rideClaimed != 1){
            throw new RideConflictException("Too late! Another driver has already accepted this ride.");
        }

        int claimed = driverRepository.claimDriver(
                loggedInDriver.getDriverId(),
                VehicleStatus.ON_RIDE,
                VehicleStatus.AVAILABLE
        );

        if(claimed != 1){
            throw new RideConflictException("Driver is not available or already on another ride.");
        }

        Rides updatedRide = rideRepository.findById(rideId).get();
        RideResponse response = RideMapper.mapToResponse(updatedRide);

        messagingTemplate.convertAndSend("/topic/ride/" + rideId, response);
        log.info("Ride {} ACCEPTED by Driver {}. Final Fare locked. Notified Rider.", rideId, loggedInDriver.getDriverId());

        List<RideNotifiedDriver> allNotified = rideNotifiedDriverRepository.findByRideId(rideId);
        Map<String, Object> rideTakenEvent = new HashMap<>();
        rideTakenEvent.put("type", "RIDE_TAKEN");
        rideTakenEvent.put("rideId", rideId);

        for (RideNotifiedDriver nd : allNotified) {
            if (nd.getDriverId() != loggedInDriver.getDriverId()) {
                messagingTemplate.convertAndSend("/topic/driver/" + nd.getDriverId(), rideTakenEvent);
            }
        }

        return  response;
    }

    @Transactional
    public Rides completeRide(int rideId, String driverEmail){

        Rides ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Error: Ride with ID " + rideId + " not found!"));

        Driver loggedInDriver = driverRepository.findByUser_Email(driverEmail).orElseThrow(
                () -> new ResourceNotFoundException("Error: Driver profile not found!"
        ));

        if(ride.getVehicle() == null || ride.getVehicle().getDriver() == null ||
        ride.getVehicle().getDriver().getDriverId() != loggedInDriver.getDriverId()){
            throw new RideConflictException("Error: Unauthorized! You can only complete rides assigned to you.");
        }

        if(ride.getRideStatus() == RideStatus.COMPLETED){
            throw new RuntimeException("This ride is already marked as COMPLETED.");
        }

        if(ride.getRideStatus() != RideStatus.ACCEPTED && ride.getRideStatus() != RideStatus.IN_PROGRESS){
            throw new RideConflictException("Error: Cannot complete a ride with status " + ride.getRideStatus());
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

    public List<Rides> getRideHistory(int riderId, String riderEmail){

        Riders rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new RuntimeException("Error: Rider with ID " + riderId + " not found!"));

        if(!rider.getUsers().getEmail().equals(riderEmail)){
            throw new RideConflictException("Error: Unauthorized! You can only view your own ride history.");
        }

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
