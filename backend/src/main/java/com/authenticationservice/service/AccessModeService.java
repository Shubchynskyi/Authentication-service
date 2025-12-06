package com.authenticationservice.service;

import com.authenticationservice.model.AccessMode;
import com.authenticationservice.model.AccessModeSettings;
import com.authenticationservice.repository.AccessModeSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for managing access mode settings.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessModeService {

    private final AccessModeSettingsRepository accessModeSettingsRepository;

    private static final Long SETTINGS_ID = 1L;

    /**
     * Gets current access mode.
     * Defaults to WHITELIST if settings not found.
     * 
     * @return Current access mode
     */
    @Transactional(readOnly = true)
    public AccessMode getCurrentMode() {
        Optional<AccessModeSettings> settings = accessModeSettingsRepository.findById(SETTINGS_ID);
        return settings.map(AccessModeSettings::getMode)
                .orElse(AccessMode.WHITELIST);
    }

    /**
     * Gets access mode settings.
     * 
     * @return Access mode settings or null if not found
     */
    @Transactional(readOnly = true)
    public AccessModeSettings getSettings() {
        return accessModeSettingsRepository.findById(SETTINGS_ID).orElse(null);
    }
}

