package com.ridehailing.dto;

import lombok.Data;

@Data
public class LocationUpdateDto {

    private int rideId;

    private int driverId;

    private double latitude;

    private double longitude;
}
