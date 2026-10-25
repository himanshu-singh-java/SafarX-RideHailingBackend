package com.ridehailing.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ride_skipped_log")
public class RideSkippedLog {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private int id;

    @Column(name = "ride_id", nullable = false)
    private int rideId;

    @Column(name = "driver_id")
    private int driverId;

    public RideSkippedLog(){

    }

    public RideSkippedLog(int rideId, int driverId){
        this.rideId = rideId;
        this.driverId = driverId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRideId() {
        return rideId;
    }

    public void setRideId(int rideId) {
        this.rideId = rideId;
    }

    public int getDriverId() {
        return driverId;
    }

    public void setDriverId(int driverId) {
        this.driverId = driverId;
    }
}
