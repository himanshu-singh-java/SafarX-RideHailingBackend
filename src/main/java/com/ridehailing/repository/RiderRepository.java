package com.ridehailing.repository;

import com.ridehailing.model.Riders;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RiderRepository extends JpaRepository<Riders, Integer> {

    Optional<Riders> findByUsers_Email(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Riders r WHERE r.users.email = :email")
    Optional<Riders> findWithLockByUsers_Email(@Param("email") String email);
}
