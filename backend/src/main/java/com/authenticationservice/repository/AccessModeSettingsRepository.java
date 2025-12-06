package com.authenticationservice.repository;

import com.authenticationservice.model.AccessModeSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessModeSettingsRepository extends JpaRepository<AccessModeSettings, Long> {
}

