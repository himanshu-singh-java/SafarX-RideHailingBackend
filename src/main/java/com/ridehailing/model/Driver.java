package com.ridehailing.model;

import jakarta.persistence.*;

@Entity
@Table(name = "drivers")
public class Driver {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "driver_id")
    private int driverId;

    @Column(name = "driver_name")
    private String driverName;

    @Column(name = "rating")
    private double rating;

    @Column(name = "current_latitude")
    private Double currentLat;

    @Column(name = "current_longitude")
    private Double currentLng;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private VehicleStatus driverStatus;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private Users user;

    @OneToOne
    @JoinColumn(name = "vehicle_id", referencedColumnName = "vehicle_id")
    private Vehicles vehicle;

    public Driver(){

    }

    public int getDriverId() {
        return driverId;
    }

    public void setDriverId(int driverId) {
        this.driverId = driverId;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public Double getCurrentLat() {
        return currentLat;
    }

    public void setCurrentLat(Double currentLat) {
        this.currentLat = currentLat;
    }

    public Double getCurrentLng() {
        return currentLng;
    }

    public void setCurrentLng(Double currentLng) {
        this.currentLng = currentLng;
    }

    public VehicleStatus getDriverStatus() {
        return driverStatus;
    }

    public void setDriverStatus(VehicleStatus driverStatus) {
        this.driverStatus = driverStatus;
    }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    public Vehicles getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicles vehicle) {
        this.vehicle = vehicle;
    }
}
