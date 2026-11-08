package com.ridehailing.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@Setter
@Table(name = "riders")
public class Riders {

    @Id
    @Column(name = "rider_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int riderID;

    @OneToOne(fetch = FetchType.LAZY)
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
}
