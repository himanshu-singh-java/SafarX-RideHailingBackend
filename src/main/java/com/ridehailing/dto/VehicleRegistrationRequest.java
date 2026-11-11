package com.ridehailing.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VehicleRegistrationRequest {

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

    public DriverOnboardingRequest toOnboardingRequest() {
        DriverOnboardingRequest req = new DriverOnboardingRequest();
        req.setVehicleType(this.vehicleType);
        req.setPlateNumber(this.plateNumber);
        req.setCarModel(this.carModel);
        req.setCarCategory(this.carCategory);
        req.setCarPassengerCapacity(this.carPassengerCapacity);
        req.setIsAcAvailable(this.isAcAvailable);
        req.setHasLuggageCarrier(this.hasLuggageCarrier);
        req.setHasWifiOrScreen(this.hasWifiOrScreen);
        req.setBikeModel(this.bikeModel);
        req.setBikeType(this.bikeType);
        req.setHasExtraHelmet(this.hasExtraHelmet);
        req.setIsParcelDeliveryEnabled(this.isParcelDeliveryEnabled);
        req.setAutoModel(this.autoModel);
        req.setPermitType(this.permitType);
        req.setAutoPassengerCapacity(this.autoPassengerCapacity);
        req.setIsSharedAuto(this.isSharedAuto);
        return req;
    }
}
