package com.ridehailing.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rides")
public class Rides {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ride_id")
    private int rideId;

    @JoinColumn(name = "rider_id", nullable = false)
    @ManyToOne
    private Riders rider;

    @JoinColumn(name = "vehicle_id")
    @ManyToOne
    private Vehicles vehicle;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "fare")
    private Double fare;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private RideStatus rideStatus;

    @Column(name = "razorpay_order_id")
    private String razorPayOrderId;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "payment_status")
    private String paymentStatus = "PENDING";

    @Column(name = "pickup_latitude")
    private Double pickupLat;

    @Column(name = "pickup_longitude")
    private Double pickupLng;

    @Column(name = "drop_latitude")
    private Double dropLat;

    @Column(name = "drop_longitude")
    private Double dropLng;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "current_radius_index")
    private int currentRadiusIndex = 0;

    @Column(name = "last_broadcast_at")
    private LocalDateTime lastBroadcastAt;

    public Rides(){

    }

    public int getRideId() {
        return rideId;
    }

    public void setRideId(int rideId) {
        this.rideId = rideId;
    }

    public Riders getRider() {
        return rider;
    }

    public void setRider(Riders rider) {
        this.rider = rider;
    }

    public Vehicles getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicles vehicle) {
        this.vehicle = vehicle;
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

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Double getFare() {
        return fare;
    }

    public void setFare(Double fare) {
        this.fare = fare;
    }

    public RideStatus getRideStatus() {
        return rideStatus;
    }

    public void setRideStatus(RideStatus rideStatus) {
        this.rideStatus = rideStatus;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public int getCurrentRadiusIndex() {
        return currentRadiusIndex;
    }

    public void setCurrentRadiusIndex(int currentRadiusIndex) {
        this.currentRadiusIndex = currentRadiusIndex;
    }

    public LocalDateTime getLastBroadcastAt() {
        return lastBroadcastAt;
    }

    public void setLastBroadcastAt(LocalDateTime lastBroadcastAt) {
        this.lastBroadcastAt = lastBroadcastAt;
    }

    public String getRazorPayOrderId() {
        return razorPayOrderId;
    }

    public void setRazorPayOrderId(String razorPayOrderId) {
        this.razorPayOrderId = razorPayOrderId;
    }

    @Override
    public String toString() {
        return "Rides{" +
                "rideId=" + rideId +
                ", rider=" + rider +
                ", vehicle=" + vehicle +
                ", distanceKm=" + distanceKm +
                ", fare=" + fare +
                ", rideStatus=" + rideStatus +
                ", razorPayOrderId='" + razorPayOrderId + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", pickupLat=" + pickupLat +
                ", pickupLng=" + pickupLng +
                ", dropLat=" + dropLat +
                ", dropLng=" + dropLng +
                ", vehicleType='" + vehicleType + '\'' +
                ", currentRadiusIndex=" + currentRadiusIndex +
                ", lastBroadcastAt=" + lastBroadcastAt +
                '}';
    }
}
