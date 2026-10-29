package com.ridehailing.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ride_notified_drivers")
public class RideNotifiedDriver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int rideId;
    private int driverId;

    public RideNotifiedDriver() {
    }

    public RideNotifiedDriver(int rideId, int driverId) {
        this.rideId = rideId;
        this.driverId = driverId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getRideId() { return rideId; }
    public void setRideId(int rideId) { this.rideId = rideId; }
    public int getDriverId() { return driverId; }
    public void setDriverId(int driverId) { this.driverId = driverId; }
}