package com.ridehailing.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "riders")
public class Riders {

    @Id
    @Column(name = "rider_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int riderID;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private Users users;

    @Column(name = "name")
    private String riderName;

    @Column(name = "phone")
    private String phoneNum;

    @Column(name = "rating")
    private Double riderRating;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Riders(){

    }

    public int getRiderID() {
        return riderID;
    }

    public void setRiderID(int riderID) {
        this.riderID = riderID;
    }

    public String getRiderName() {
        return riderName;
    }

    public void setRiderName(String riderName) {
        this.riderName = riderName;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public Double getRiderRating() {
        return riderRating;
    }

    public void setRiderRating(Double riderRating) {
        this.riderRating = riderRating;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }

    @Override
    public String toString() {
        return "Riders{" +
                "riderID=" + riderID +
                ", users=" + users +
                ", riderName='" + riderName + '\'' +
                ", phoneNum='" + phoneNum + '\'' +
                ", riderRating=" + riderRating +
                ", createdAt=" + createdAt +
                '}';
    }
}
