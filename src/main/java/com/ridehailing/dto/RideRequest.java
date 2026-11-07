package com.ridehailing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RideRequest {

    private int riderId;

    @NotBlank(message = "Vehicle type is required")
    private String vehicleType;

    @NotNull(message = "Pickup latitude is required")
    @Min(value = -90, message = "Invalid latitude")
    @Max(value = 90, message = "Invalid latitude")
    private Double pickupLat;

    @NotNull(message = "Pickup longitude is required")
    @Min(value = -180, message = "Invalid longitude")
    @Max(value = 190, message = "Invalid longitude")
    private Double pickupLng;

    @NotNull(message = "Pickup latitude is required")
    @Min(value = -90, message = "Invalid latitude")
    @Max(value = 90, message = "Invalid latitude")
    private Double dropLat;

    @NotNull(message = "Pickup longitude is required")
    @Min(value = -180, message = "Invalid longitude")
    @Max(value = 190, message = "Invalid longitude")
    private Double dropLng;

    public int getRiderId() {
        return riderId;
    }

    public void setRiderId(int riderId) {
        this.riderId = riderId;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Double getPickupLat() {
        return pickupLat;
    }

    public void setPickupLat(Double pickupLat) {
        this.pickupLat = pickupLat;
    }

    public Double getPickupLng() {
        return pickupLng;
    }

    public void setPickupLng(Double pickupLng) {
        this.pickupLng = pickupLng;
    }

    public Double getDropLat() {
        return dropLat;
    }

    public void setDropLat(Double dropLat) {
        this.dropLat = dropLat;
    }

    public Double getDropLng() {
        return dropLng;
    }

    public void setDropLng(Double dropLng) {
        this.dropLng = dropLng;
    }
}
