package com.ridehailing.repository;

import com.ridehailing.model.RideSkippedLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RideSkippedLogRepository extends JpaRepository<RideSkippedLog, Integer> {
}
