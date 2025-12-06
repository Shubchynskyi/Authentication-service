package com.authenticationservice.repository;

import com.authenticationservice.model.AccessListChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessListChangeLogRepository extends JpaRepository<AccessListChangeLog, Long> {
}

