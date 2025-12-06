package com.authenticationservice.service;

import com.authenticationservice.model.AccessMode;
import com.authenticationservice.model.AccessModeSettings;
import com.authenticationservice.repository.AccessModeSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initializes access mode settings on application startup.
 * Creates default WHITELIST mode if no settings exist.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessModeInitializer {

    private final AccessModeSettingsRepository accessModeSettingsRepository;

    @Value("${access.mode.default:WHITELIST}")
    private String defaultAccessMode;

    @Transactional
    public void initialize() {
        log.info("Initializing access mode settings...");
        
        Long settingsId = 1L;
        AccessModeSettings existingSettings = accessModeSettingsRepository.findById(settingsId).orElse(null);
        
        if (existingSettings == null) {
            AccessMode mode = AccessMode.valueOf(defaultAccessMode.toUpperCase());
            AccessModeSettings newSettings = new AccessModeSettings();
            newSettings.setId(settingsId);
            newSettings.setMode(mode);
            accessModeSettingsRepository.save(newSettings);
            log.info("Created default access mode settings with mode: {}", mode);
        } else {
            log.info("Access mode settings already exist with mode: {}", existingSettings.getMode());
        }
    }
}

