package com.ridehailing.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;


@Entity
@Table(name = "cars")
public class Car extends Vehicles{

    @Column(name = "car_model")
    private String carModel;

    @Column(name = "car_category")
    private String carCategory;

    @Column(name = "passenger_capacity")
    private int passengerCapacity;

    @Column(name = "is_ac_available")
    private Boolean isAcAvailable;

    @Column(name = "has_luggage_carrier")
    private Boolean hasLuggageCarrier;

    @Column(name = "has_wifi_or_screen")
    private Boolean hasWifiOrScreen;

    public Car(){

    }

    @Override
    public double calculateFare(double distanceKm){
        double baseFare = 50.0;
        double perKmRate = 12.0;

        if(this.carCategory != null && this.carCategory.equalsIgnoreCase("SUV")){
            perKmRate = 18.0;
            baseFare = 100.0;
        }
        return baseFare + (distanceKm * perKmRate);
    }

    public String getCarModel() {
        return carModel;
    }

    public void setCarModel(String carModel) {
        this.carModel = carModel;
    }

    public String getCarCategory() {
        return carCategory;
    }

    public void setCarCategory(String carCategory) {
        this.carCategory = carCategory;
    }

    public int getPassengerCapacity() {
        return passengerCapacity;
    }

    public void setPassengerCapacity(int passengerCapacity) {
        this.passengerCapacity = passengerCapacity;
    }

    public Boolean isAcAvailable() {
        return isAcAvailable;
    }

    public void setAcAvailable(Boolean acAvailable) {
        isAcAvailable = acAvailable;
    }

    public Boolean isHasLuggageCarrier() {
        return hasLuggageCarrier;
    }

    public void setHasLuggageCarrier(Boolean hasLuggageCarrier) {
        this.hasLuggageCarrier = hasLuggageCarrier;
    }

    public Boolean getHasWifiOrScreen() {
        return hasWifiOrScreen;
    }

    public void setHasWifiOrScreen(Boolean hasWifiOrScreen) {
        this.hasWifiOrScreen = hasWifiOrScreen;
    }
}
