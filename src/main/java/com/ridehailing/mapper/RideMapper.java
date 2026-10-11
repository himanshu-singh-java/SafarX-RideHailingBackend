package com.ridehailing.mapper;

import com.ridehailing.dto.RideRequest;
import com.ridehailing.dto.RideResponse;
import com.ridehailing.model.Rides;

public class RideMapper {

    public static Rides mapToEntity(RideRequest request){

        Rides rides = new Rides();

        rides.setPickupLat(request.getPickupLat());
        rides.setPickupLng(request.getPickupLng());
        rides.setDropLat(request.getDropLat());
        rides.setDropLng(request.getDropLng());

        return rides;
    }

    public static RideResponse mapToResponse(Rides rides){
        RideResponse response = new RideResponse();

        response.setRideId(rides.getRideId());

        response.setPickupLat(rides.getPickupLat());
        response.setPickupLng(rides.getPickupLng());
        response.setDropLat(rides.getDropLat());
        response.setDropLng(rides.getDropLng());

        response.setFare(rides.getFare());
        response.setStatus(rides.getRideStatus().toString());

        return response;
    }
}
