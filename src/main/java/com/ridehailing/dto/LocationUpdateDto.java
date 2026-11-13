package com.ridehailing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocationUpdateDto {

    private int rideId;
    private int driverId;
    private double latitude;
    private double longitude;
    private String driverName;
    private String vehicleNumber;
    private String vehicleModel;
    private String vehicleType;
    private Double rating;
}
