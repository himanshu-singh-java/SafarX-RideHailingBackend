package com.ridehailing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DriverOnboardingRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    @NotBlank(message = "Vehicle type is required")
    private String vehicleType;

    @NotBlank(message = "Plate number is required")
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
