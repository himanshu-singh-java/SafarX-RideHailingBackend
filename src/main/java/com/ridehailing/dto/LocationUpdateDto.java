package com.ridehailing.dto;

import lombok.Data;

@Data
public class LocationUpdateDto {

    private Long rideId;

    private Long driverId;

    private double latitude;

    private double longitude;
}
