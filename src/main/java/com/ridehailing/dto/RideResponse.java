package com.ridehailing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RideResponse {

    private int rideId;
    private Double pickupLat;
    private Double pickupLng;
    private Double dropLat;
    private Double dropLng;
    private double fare;
    private String status;

    private Integer driverId;
    private String driverName;
    private Double driverRating;
    private String driverPhone;
    private String vehicleNumber;
    private String plateNumber;
    private String vehicleModel;
    private String vehicleType;
    private DriverDetails driver;

    public RideResponse(int rideId, Double pickupLat, Double pickupLng, Double dropLat, Double dropLng, double fare, String status) {
        this.rideId = rideId;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.dropLat = dropLat;
        this.dropLng = dropLng;
        this.fare = fare;
        this.status = status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DriverDetails {
        private Integer id;
        private Integer driverId;
        private String name;
        private String driverName;
        private Double rating;
        private String phone;
        private String vehicleNumber;
        private String plateNumber;
        private String model;
        private String vehicleModel;
        private String vehicleType;
    }
}
