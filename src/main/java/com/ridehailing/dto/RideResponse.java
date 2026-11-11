package com.ridehailing.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RideResponse {

    private final int rideId;
    private final Double pickupLat;
    private final Double pickupLng;
    private final Double dropLat;
    private final Double dropLng;
    private final double fare;
    private final String status;
}
