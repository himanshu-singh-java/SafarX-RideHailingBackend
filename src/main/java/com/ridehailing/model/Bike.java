package com.ridehailing.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "bikes")
public class Bike extends Vehicles {

    @Column(name = "bike_model")
    private String bikeModel;

    @Column(name = "bike_type")
    private String bikeType;

    @Column(name = "passenger_capacity")
    private int passengerCapacity;

    @Column(name = "has_extra_helmet")
    private Boolean hasExtraHelmet;

    @Column(name = "is_parcel_delivery_enabled")
    private Boolean isParcelDeliveryEnabled;

    public Bike(){

    }

    @Override
    public double calculateFare(double distanceKm){
        double baseFare = 20;
        double perKmRate = 6;
        return baseFare + (perKmRate * distanceKm);
    }

    @Override
    public String toString() {
        return "Bike{" +
                "bikeModel='" + bikeModel + '\'' +
                ", bikeType='" + bikeType + '\'' +
                ", passengerCapacity=" + passengerCapacity +
                ", hasExtraHelmet=" + hasExtraHelmet +
                ", isParcelDeliveryEnabled=" + isParcelDeliveryEnabled +
                '}';
    }
}
