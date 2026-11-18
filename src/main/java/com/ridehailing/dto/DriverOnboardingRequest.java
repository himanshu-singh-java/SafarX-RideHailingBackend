package com.ridehailing.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DriverOnboardingRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotNull(message = "Latitude is required")
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @NotNull(message = "Longitude is required")
    private Double longitude;

    @NotBlank(message = "Vehicle type is required")
    private String vehicleType;

    @NotBlank(message = "Plate number is required")
    private String plateNumber;

    @NotBlank(message = "Otp is required")
    private String otp;

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
