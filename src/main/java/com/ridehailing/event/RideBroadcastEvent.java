package com.ridehailing.event;


import com.ridehailing.dto.RideResponse;

import java.util.List;

public class RideBroadcastEvent {
    private int rideId;
    private RideResponse rideResponse;
    private List<Integer> driversId;

    public RideBroadcastEvent(int rideId, RideResponse rideResponse, List<Integer> driversId){
        this.rideId = rideId;
        this.rideResponse = rideResponse;
        this.driversId = driversId;
    }

    public int getRideId() {
        return rideId;
    }

    public RideResponse getRideResponse() {
        return rideResponse;
    }

    public List<Integer> getDriversId() {
        return driversId;
    }
}
