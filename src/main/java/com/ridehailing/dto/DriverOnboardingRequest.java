package com.ridehailing.dto;

import lombok.Data;

@Data
public class DriverOnboardingRequest {

    private String email;
    private String password;
    private String fullName;
    private String phone;


    private String vehicleType;
    private String plateNumber;

    private String carModel;
    private String carCategory;
    private Integer carPassengerCapacity;
    private Boolean isAcAvailable;
    private Boolean hasLuggageCarrier;
    private Boolean hasWifiOrScreen;


    private String bikeModel;
    private String bikeType;
    private Boolean hasExtraHelmet;
    private Boolean isParcelDeliveryEnabled;

    private String autoModel;
    private String permitType;
    private Integer autoPassengerCapacity;
    private Boolean isSharedAuto;

}
