package com.ridehailing.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "vehicles")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Vehicles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id")
    private int vehicleId;

    @Column(name = "plate_number", nullable = false, unique = true)
    private String plateNumber;

    @JsonIgnore
    @OneToOne(mappedBy = "vehicle", fetch = FetchType.LAZY)
    private Driver driver;

    public Vehicles(){

    }

    public abstract double calculateFare(double distanceKm);

    public abstract VehicleCategory getVehicleCategory();

    public abstract String getVehicleModelName();

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }
}
