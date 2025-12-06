package com.authenticationservice.repository;

import com.authenticationservice.model.AccessModeChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessModeChangeLogRepository extends JpaRepository<AccessModeChangeLog, Long> {
}

