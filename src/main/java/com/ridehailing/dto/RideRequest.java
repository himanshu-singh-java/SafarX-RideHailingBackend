package com.ridehailing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class RideRequest {

    @NotBlank(message = "Vehicle type is required")
    private String vehicleType;

    @NotNull(message = "Pickup latitude is required")
    @Min(value = -90, message = "Invalid pickup latitude")
    @Max(value = 90, message = "Invalid pickup latitude")
    private Double pickupLat;

    @NotNull(message = "Pickup longitude is required")
    @Min(value = -180, message = "Invalid pickup longitude")
    @Max(value = 180, message = "Invalid pickup longitude")
    private Double pickupLng;

    @NotNull(message = "Drop latitude is required")
    @Min(value = -90, message = "Invalid drop latitude")
    @Max(value = 90, message = "Invalid drop latitude")
    private Double dropLat;

    @NotNull(message = "Drop longitude is required")
    @Min(value = -180, message = "Invalid drop longitude")
    @Max(value = 180, message = "Invalid drop longitude")
    private Double dropLng;
}
