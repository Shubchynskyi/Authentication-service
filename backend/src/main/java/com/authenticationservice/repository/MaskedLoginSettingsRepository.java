package com.authenticationservice.repository;

import com.authenticationservice.model.MaskedLoginSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaskedLoginSettingsRepository extends JpaRepository<MaskedLoginSettings, Long> {
}

