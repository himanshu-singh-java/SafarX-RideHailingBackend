package com.ridehailing.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

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

    public String getBikeModel() {
        return bikeModel;
    }

    public void setBikeModel(String bikeModel) {
        this.bikeModel = bikeModel;
    }

    public String getBikeType() {
        return bikeType;
    }

    public void setBikeType(String bikeType) {
        this.bikeType = bikeType;
    }

    public int getPassengerCapacity() {
        return passengerCapacity;
    }

    public void setPassengerCapacity(int passengerCapacity) {
        this.passengerCapacity = passengerCapacity;
    }

    public Boolean HasExtraHelmet() {
        return hasExtraHelmet;
    }

    public void setHasExtraHelmet(Boolean hasExtraHelmet) {
        this.hasExtraHelmet = hasExtraHelmet;
    }

    public Boolean isParcelDeliveryEnabled() {
        return isParcelDeliveryEnabled;
    }

    public void setParcelDeliveryEnabled(Boolean parcelDeliveryEnabled) {
        isParcelDeliveryEnabled = parcelDeliveryEnabled;
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
