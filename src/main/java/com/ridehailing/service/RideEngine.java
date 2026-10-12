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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ridehailing.repository.RideRepository;
import com.ridehailing.repository.RiderRepository;
import com.ridehailing.repository.VehicleRepository;
import com.ridehailing.util.LocationUtils;

import java.util.List;

@Slf4j
@Service
@Transactional
public class RideEngine {

    private VehicleRepository vehicleRepository;
    private RideRepository rideRepository;
    private RiderRepository riderRepository;
    private DriverRepository driverRepository;

    @Autowired
    public RideEngine(VehicleRepository vehicleRepository,
                      RideRepository rideRepository,
                      RiderRepository riderRepository, DriverRepository driverRepository){
        this.vehicleRepository = vehicleRepository;
        this.rideRepository = rideRepository;
        this.riderRepository = riderRepository;
        this.driverRepository = driverRepository;
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
        Vehicles assignedVehicle = null;

        for(int radius : searchRadii){
            log.info("Searching for Nearest " + request.getVehicleType());

            Vehicles nearestVehicle = vehicleRepository.findNearestAvailableVehicle(request.getVehicleType(),
                    request.getPickupLat(), request.getPickupLng(), radius);

            if (nearestVehicle == null) {
                log.info("No {} found within {} km, expanding search...", request.getVehicleType(), radius);
                continue;
            }

            int claimed = driverRepository.claimDriver(
                    nearestVehicle.getDriver().getDriverId(),
                    VehicleStatus.ON_RIDE,
                    VehicleStatus.AVAILABLE
            );

            if(claimed == 1){
                log.info("Success: Driver {} successfully claimed within {} km!", nearestVehicle.getDriver().getDriverName(), radius);
                assignedVehicle = nearestVehicle;
                break;
            }
            else {
                log.warn("Race condition avoided: Driver was just booked by someone else. Searching next...");
            }
        }


        if(assignedVehicle == null){
            throw new ResourceNotFoundException("Error: No available " + request.getVehicleType() + " found right now.");
        }

        double estimatedDistance = LocationUtils.calculateDistance(request.getPickupLat(),
                request.getPickupLng(), request.getDropLat(), request.getDropLng());

        Double estimatedFare = assignedVehicle.calculateFare(estimatedDistance);

        Driver driver = assignedVehicle.getDriver();
        if(driver != null) {
            driver.setCurrentLat(request.getDropLat());
            driver.setCurrentLng(request.getDropLng());
        }

        Rides newRide = RideMapper.mapToEntity(request);
        newRide.setRider(rider);
        newRide.setVehicle(assignedVehicle);
        newRide.setDistanceKm(estimatedDistance);
        newRide.setFare(estimatedFare);
        newRide.setRideStatus(RideStatus.ACCEPTED);
        rideRepository.save(newRide);

        return RideMapper.mapToResponse(newRide);
    }

    public Rides completeRide(int rideId){

        Rides ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Error: Ride with ID " + rideId + " not found!"));

        if(ride.getRideStatus() == RideStatus.COMPLETED){
            throw new RuntimeException("This ride is already marked as COMPLETED.");
        }

        ride.setRideStatus(RideStatus.COMPLETED);

        Vehicles vehicles = ride.getVehicle();
        if (vehicles != null && vehicles.getDriver() != null) {
            vehicles.getDriver().setDriverStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicles);
        }

        rideRepository.save(ride);

        return ride;
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

        Vehicles vehicles = activeRide.getVehicle();
        if(vehicles != null && vehicles.getDriver() != null){
            vehicles.getDriver().setDriverStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicles);
        }
        return rideRepository.save(activeRide);
    }
}
