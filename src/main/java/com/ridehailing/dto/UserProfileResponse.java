package com.ridehailing.dto;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProfileResponse(
        String email,
        String role,
        String name,
        Integer riderId,
        Integer driverId,
        String status,
        String vehicleType,
        String vehicleModel,
        String plateNumber,
        Double rating
) {
    public UserProfileResponse(String email, String role, String name, Integer riderId, Integer driverId, String status) {
        this(email, role, name, riderId, driverId, status, null, null, null, null);
    }
}
