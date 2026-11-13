package com.ridehailing.service;

import com.ridehailing.dto.RideRequest;
import com.ridehailing.dto.RideResponse;
import com.ridehailing.event.RideBroadcastEvent;
import com.ridehailing.event.RideNotificationEvent;
import com.ridehailing.exception.ForbiddenException;
import com.ridehailing.exception.ResourceNotFoundException;
import com.ridehailing.exception.RideConflictException;
import com.ridehailing.mapper.RideMapper;
import com.ridehailing.model.*;
import com.ridehailing.repository.*;
import com.ridehailing.util.LocationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class RideEngine {

    private final VehicleRepository vehicleRepository;
    private final RideRepository rideRepository;
    private final RiderRepository riderRepository;
    private final DriverRepository driverRepository;
    private final RideNotifiedDriverRepository rideNotifiedDriverRepository;
    private final ApplicationEventPublisher publisher;

    private final int[] SEARCH_RADII = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    public static final int WAVE_TIMEOUT_SECONDS = 20;

    public RideEngine(VehicleRepository vehicleRepository,
                      RideRepository rideRepository,
                      RiderRepository riderRepository,
                      DriverRepository driverRepository,
                      RideNotifiedDriverRepository rideNotifiedDriverRepository,
                      ApplicationEventPublisher publisher){

        this.vehicleRepository = vehicleRepository;
        this.rideRepository = rideRepository;
        this.riderRepository = riderRepository;
        this.driverRepository = driverRepository;
        this.rideNotifiedDriverRepository = rideNotifiedDriverRepository;
        this.publisher = publisher;
    }

    private VehicleCategory parseVehicleCategory(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            throw new IllegalArgumentException("Vehicle type is required.");
        }
        try {
            return VehicleCategory.valueOf(rawType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid vehicle type: " + rawType);
        }
    }

    private double calculateQuotedFare(VehicleCategory category, double distanceKm) {
        double rawFare = switch (category) {
            case CAR -> 50.0 + (distanceKm * 12.0);
            case BIKE -> 20.0 + (distanceKm * 6.0);
            case AUTO -> 50.0 + (distanceKm * 15.0);
        };
        return Math.round(rawFare * 100.0) / 100.0;
    }

    public RideResponse bookRide(RideRequest request, String email){

        VehicleCategory category = parseVehicleCategory(request.getVehicleType());

        double estimatedDistance = LocationUtils.calculateDistance(
                request.getPickupLat(), request.getPickupLng(),
                request.getDropLat(), request.getDropLng()
        );

        if (estimatedDistance < 0.05) {
            throw new IllegalArgumentException("Error: Pickup and drop-off locations cannot be identical or too close.");
        }

        Riders rider = riderRepository.findWithLockByUsers_Email(email)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Rider profile not found for this user!"));

        boolean hasActiveRide = rideRepository.existsByRider_RiderIDAndRideStatusIn(
                rider.getRiderID(), List.of(RideStatus.REQUESTED, RideStatus.ACCEPTED, RideStatus.IN_PROGRESS)
        );

        if (hasActiveRide) {
            throw new RideConflictException("Error: You already have an active ride! Please complete or cancel it first.");
        }

        double quotedFare = calculateQuotedFare(category, estimatedDistance);

        Rides newRide = RideMapper.mapToEntity(request);
        newRide.setRider(rider);
        newRide.setVehicle(null);
        newRide.setDistanceKm(estimatedDistance);
        newRide.setFare(quotedFare);
        newRide.setRideStatus(RideStatus.REQUESTED);
        newRide.setVehicleType(category.name());
        newRide.setCurrentRadiusIndex(0);
        newRide.setLastBroadcastAt(LocalDateTime.now());

        Rides savedRide = rideRepository.save(newRide);

        broadcastRide(savedRide.getRideId());

        Rides finalRide = rideRepository.findById(savedRide.getRideId()).orElseThrow();

        return RideMapper.mapToResponse(finalRide);
    }

    @Transactional
    public void broadcastRide(int rideId){

        Rides ride = rideRepository.findByIdForUpdate(rideId).orElse(null);

        if(ride == null || ride.getRideStatus() != RideStatus.REQUESTED){
            return;
        }

        int radiusIndex = ride.getCurrentRadiusIndex();
        int maxRadius = SEARCH_RADII[SEARCH_RADII.length - 1];

        List<Vehicles> allVehiclesInMaxRadius = vehicleRepository.findAllAvailableVehiclesForRide(
                ride.getVehicleType(), ride.getPickupLat(), ride.getPickupLng(), maxRadius, rideId
        );

        while (radiusIndex < SEARCH_RADII.length) {
            int currentRadius = SEARCH_RADII[radiusIndex];

            List<Vehicles> availableVehicles = new ArrayList<>();

            for(Vehicles vehicle : allVehiclesInMaxRadius){
                double dist = LocationUtils.calculateDistance(
                        ride.getPickupLat(), ride.getPickupLng(),
                        vehicle.getDriver().getCurrentLat(), vehicle.getDriver().getCurrentLng()
                );

                if(dist <= currentRadius){
                    availableVehicles.add(vehicle);
                }
            }

            if (availableVehicles.isEmpty()) {
                radiusIndex++;
                continue;
            }

            double fareToSave = (ride.getFare() != null && ride.getFare() > 0.0)
                    ? ride.getFare()
                    : calculateQuotedFare(parseVehicleCategory(ride.getVehicleType()), ride.getDistanceKm());

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

            ride = rideRepository.findByIdForUpdate(rideId).orElseThrow();
            RideResponse response = RideMapper.mapToResponse(ride);

            List<Integer> driversToNotify = new ArrayList<>();

            for (Vehicles vehicles : availableVehicles) {
                int driverId = vehicles.getDriver().getDriverId();
                if (!rideNotifiedDriverRepository.existsByRideIdAndDriverId(rideId, driverId)) {
                    rideNotifiedDriverRepository.save(new RideNotifiedDriver(rideId, driverId));
                }
                driversToNotify.add(driverId);
            }

            publisher.publishEvent(new RideBroadcastEvent(rideId, response, driversToNotify));

            log.info("Ride {} broadcasted to {} drivers in {} km radius.", rideId, availableVehicles.size(), currentRadius);
            return;
        }

        log.info("No drivers accepted in max radius for ride {}", rideId);
        cancelRide(rideId);
    }

    @Transactional
    public void handleRideTimeout(int rideId){

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusSeconds(WAVE_TIMEOUT_SECONDS);
        int updated = rideRepository.incrementRadiusAndClaimTimeout(rideId, now, cutoff);

        if(updated == 0){
            return;
        }

        Rides ride = rideRepository.findByIdForUpdate(rideId).orElse(null);
        if(ride == null || ride.getRideStatus() != RideStatus.REQUESTED){
            return;
        }

        log.info("Timeout for ride {} at radius index {}. Expanding radius.", rideId, ride.getCurrentRadiusIndex());

        List<RideNotifiedDriver> notifiedDrivers = rideNotifiedDriverRepository.findByRideId(rideId);
        Map<String, Object> timeoutEvent = Map.of("type", "TIMEOUT", "rideId", rideId);

        for(RideNotifiedDriver skipped : notifiedDrivers){
            publisher.publishEvent(new RideNotificationEvent("/topic/driver/" + skipped.getDriverId(), timeoutEvent));
        }

        if(ride.getCurrentRadiusIndex() >= SEARCH_RADII.length){
            cancelRide(rideId);
            return;
        }

        broadcastRide(rideId);
    }

    @Transactional
    public RideResponse startRide(int rideId, String driverEmail){

        Rides ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Ride not found!"));

        Driver loggedInDriver = driverRepository.findByUser_Email(driverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Driver profile not found!"));

        boolean isAssignedDriver = (ride.getDriver() != null && ride.getDriver().getDriverId() == loggedInDriver.getDriverId())
                || (ride.getVehicle() != null && ride.getVehicle().getDriver() != null && ride.getVehicle().getDriver().getDriverId() == loggedInDriver.getDriverId());
        if (!isAssignedDriver) {
            throw new ForbiddenException("Error: Unauthorized! You can only start rides assigned to you.");
        }

        if (ride.getRideStatus() != RideStatus.ACCEPTED){
            throw new RideConflictException("Error: Ride can only be started from ACCEPTED state. Current state: " + ride.getRideStatus());
        }

        ride.setRideStatus(RideStatus.IN_PROGRESS);
        ride.setStartTime(LocalDateTime.now());
        ride.setDriver(loggedInDriver);
        if (ride.getVehicle() != null) {
            ride.getVehicle().setDriver(loggedInDriver);
        }
        Rides savedRide = rideRepository.save(ride);

        RideResponse response = RideMapper.mapToResponse(savedRide, loggedInDriver);
        publisher.publishEvent(new RideNotificationEvent("/topic/ride/" + rideId, response));

        return response;
    }

    @Transactional
    public RideResponse cancelRide(int rideId, String riderEmail){

        Rides ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found!"));

        if(riderEmail != null && !ride.getRider().getUsers().getEmail().equals(riderEmail)){
            throw new ForbiddenException("Error: Unauthorized! You can only cancel your own rides.");
        }

        if(ride.getRideStatus() == RideStatus.IN_PROGRESS || ride.getRideStatus() == RideStatus.COMPLETED || ride.getRideStatus() == RideStatus.CANCELLED){
            throw new RideConflictException("Error: Ride is already " + ride.getRideStatus() + " and cannot be cancelled.");
        }

        if(ride.getRideStatus() == RideStatus.ACCEPTED
                && ride.getVehicle() != null && ride.getVehicle().getDriver() != null){
            Driver assignedDriver = ride.getVehicle().getDriver();
            assignedDriver.setDriverStatus(VehicleStatus.AVAILABLE);
            driverRepository.save(assignedDriver);
        }

        ride.setRideStatus(RideStatus.CANCELLED);
        Rides cancelledRide = rideRepository.save(ride);

        List<RideNotifiedDriver> notifiedDrivers = rideNotifiedDriverRepository.findByRideId(rideId);
        Map<String, Object> cancelEvent = Map.of("type", "RIDE_CANCELLED", "rideId", rideId);

        for(RideNotifiedDriver nd : notifiedDrivers){
            publisher.publishEvent(new RideNotificationEvent("/topic/driver/" + nd.getDriverId(), cancelEvent));
        }

        RideResponse response = RideMapper.mapToResponse(cancelledRide);
        publisher.publishEvent(new RideNotificationEvent("/topic/ride/" + rideId, response));
        return response;
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

        Rides rideForCheck = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found!"));

        if(rideForCheck.getRideStatus() == RideStatus.CANCELLED){
            throw new RideConflictException("Sorry, this ride has been cancelled by the rider.");
        }

        if (loggedInDriver.getUser() != null && loggedInDriver.getUser().getName() != null && !loggedInDriver.getUser().getName().isBlank()) {
            if (!loggedInDriver.getUser().getName().equals(loggedInDriver.getDriverName())) {
                loggedInDriver.setDriverName(loggedInDriver.getUser().getName().trim());
                driverRepository.save(loggedInDriver);
            }
        }

        int rideClaimed = rideRepository.claimRideAtDB(
                rideId,
                acceptingVehicle,
                loggedInDriver,
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

        Rides updatedRide = rideRepository.findByIdForUpdate(rideId).orElseThrow();
        acceptingVehicle.setDriver(loggedInDriver);
        updatedRide.setVehicle(acceptingVehicle);
        updatedRide.setDriver(loggedInDriver);
        RideResponse response = RideMapper.mapToResponse(updatedRide, loggedInDriver);

        publisher.publishEvent(new RideNotificationEvent("/topic/ride/" + rideId, response));
        log.info("Ride {} ACCEPTED by Driver {}. Fare remained locked at {}.", rideId, loggedInDriver.getDriverId(), updatedRide.getFare());

        List<RideNotifiedDriver> allNotified = rideNotifiedDriverRepository.findByRideId(rideId);
        Map<String, Object> rideTakenEvent = new HashMap<>();
        rideTakenEvent.put("type", "RIDE_TAKEN");
        rideTakenEvent.put("rideId", rideId);

        for (RideNotifiedDriver nd : allNotified) {
            if (nd.getDriverId() != loggedInDriver.getDriverId()) {
                publisher.publishEvent(new RideNotificationEvent("/topic/driver/" + nd.getDriverId(), rideTakenEvent));
            }
        }

        return response;
    }

    @Transactional
    public RideResponse completeRide(int rideId, String driverEmail){

        Rides ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Ride with ID " + rideId + " not found!"));

        Driver loggedInDriver = driverRepository.findByUser_Email(driverEmail).orElseThrow(
                () -> new ResourceNotFoundException("Error: Driver profile not found!")
        );

        boolean isAssignedDriver = (ride.getDriver() != null && ride.getDriver().getDriverId() == loggedInDriver.getDriverId())
                || (ride.getVehicle() != null && ride.getVehicle().getDriver() != null && ride.getVehicle().getDriver().getDriverId() == loggedInDriver.getDriverId());
        if (!isAssignedDriver) {
            throw new ForbiddenException("Error: Unauthorized! You can only complete rides assigned to you.");
        }

        if(ride.getRideStatus() == RideStatus.COMPLETED){
            throw new RideConflictException("This ride is already marked as COMPLETED.");
        }

        if(ride.getRideStatus() != RideStatus.IN_PROGRESS){
            throw new RideConflictException("Error: Ride must be IN_PROGRESS to complete. Current state: " + ride.getRideStatus());
        }

        ride.setRideStatus(RideStatus.COMPLETED);
        ride.setEndTime(LocalDateTime.now());
        ride.setDriver(loggedInDriver);

        Driver driver = loggedInDriver;
        driver.setDriverStatus(VehicleStatus.AVAILABLE);
        driver.setCurrentLat(ride.getDropLat());
        driver.setCurrentLng(ride.getDropLng());
        driverRepository.save(driver);

        Rides savedRide = rideRepository.save(ride);
        if (savedRide.getVehicle() != null) {
            savedRide.getVehicle().setDriver(loggedInDriver);
        }
        RideResponse compResponse = RideMapper.mapToResponse(savedRide, loggedInDriver);

        publisher.publishEvent(new RideNotificationEvent("/topic/ride/" + rideId, compResponse));

        return compResponse;
    }

    @Transactional(readOnly = true)
    public List<RideResponse> getRideHistory(int riderId, String riderEmail){

        Riders rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Rider with ID " + riderId + " not found!"));

        if(!rider.getUsers().getEmail().equals(riderEmail)){
            throw new ForbiddenException("Error: Unauthorized! You can only view your own ride history.");
        }

        return rideRepository.findByRider_RiderIDOrderByRideIdDesc(riderId)
                .stream()
                .map(RideMapper::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteRide(int rideId) {

        Rides rides = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Cannot delete. Ride with ID " + rideId + " not found!"));

        if(rides.getRideStatus() == RideStatus.ACCEPTED || rides.getRideStatus() == RideStatus.IN_PROGRESS){
            Vehicles vehicles = rides.getVehicle();

            if(vehicles != null && vehicles.getDriver() != null){
                Driver assignedDriver = vehicles.getDriver();
                assignedDriver.setDriverStatus(VehicleStatus.AVAILABLE);
                driverRepository.save(assignedDriver);
            }
        }

        rideRepository.deleteNotifiedDriversByRideId(rideId);
        rideRepository.deleteById(rideId);
    }

    @Transactional(readOnly = true)
    public List<RideResponse> getAllGlobalRides() {
        return rideRepository.findAllByOrderByRideIdDesc()
                .stream()
                .map(RideMapper::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public Rides completeRideByVehicleId(int vehicleId){

        List<Rides> activeRides = rideRepository.findActiveRidesByVehicleId(vehicleId);
        if (activeRides.isEmpty()) {
            throw new ResourceNotFoundException("No Active Ride found for Vehicle Id: " + vehicleId);
        }

        Rides activeRide = activeRides.get(0);
        if (activeRide.getRideStatus() != RideStatus.IN_PROGRESS) {
            throw new RideConflictException("Ride must be IN_PROGRESS to complete. Current state: " + activeRide.getRideStatus());
        }

        activeRide.setRideStatus(RideStatus.COMPLETED);
        activeRide.setEndTime(LocalDateTime.now());

        Vehicles vehicles = activeRide.getVehicle();
        if(vehicles != null && vehicles.getDriver() != null){
            Driver driver = vehicles.getDriver();
            driver.setDriverStatus(VehicleStatus.AVAILABLE);
            driver.setCurrentLat(activeRide.getDropLat());
            driver.setCurrentLng(activeRide.getDropLng());
            driverRepository.save(driver);
        }

        Rides savedRide = rideRepository.save(activeRide);
        publisher.publishEvent(new RideNotificationEvent("/topic/ride/" + savedRide.getRideId(), RideMapper.mapToResponse(savedRide)));
        return savedRide;
    }
}