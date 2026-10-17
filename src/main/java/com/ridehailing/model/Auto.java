package com.ridehailing.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "autos")
public class Auto extends Vehicles{

    @Column(name = "auto_model")
    private String autoModel;

    @Column(name = "permit_type")
    private String permitType;

    @Column(name = "passenger_capacity")
    private int passengerCapacity;

    @Column(name = "is_shared_auto")
    private Boolean isSharedAuto;

    public Auto(){

    }

    @Override
    public double calculateFare(double distanceKm){
        double baseFare = 50;
        double ratePerKm = 15;

        return baseFare + (distanceKm * ratePerKm);
    }

    @Override
    public String toString() {
        return "Auto{" +
                "autoModel='" + autoModel + '\'' +
                ", permitType='" + permitType + '\'' +
                ", passengerCapacity=" + passengerCapacity +
                ", isSharedAuto=" + isSharedAuto +
                '}';
    }
}
