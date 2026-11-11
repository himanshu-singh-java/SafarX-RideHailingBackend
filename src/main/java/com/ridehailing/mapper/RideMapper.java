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

        return new RideResponse(
                rides.getRideId(),
                rides.getPickupLat(),
                rides.getPickupLng(),
                rides.getDropLat(),
                rides.getDropLng(),
                rides.getFare(),
                rides.getRideStatus() != null ? rides.getRideStatus().name() : "UNKNOWN"
        );
    }
}
