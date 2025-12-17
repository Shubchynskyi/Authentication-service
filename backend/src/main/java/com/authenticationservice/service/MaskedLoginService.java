package com.authenticationservice.service;

import com.authenticationservice.model.MaskedLoginSettings;
import com.authenticationservice.repository.MaskedLoginSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for managing masked login settings.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaskedLoginService {

    private final MaskedLoginSettingsRepository maskedLoginSettingsRepository;

    private static final Long SETTINGS_ID = 1L;
    private static final int MIN_TEMPLATE_ID = 1;
    private static final int MAX_TEMPLATE_ID = 10;
    /**
     * Gets current masked login settings.
     * 
     * @return Masked login settings or null if not found
     */
    @Transactional(readOnly = true)
    public MaskedLoginSettings getSettings() {
        MaskedLoginSettings settings = maskedLoginSettingsRepository.findById(SETTINGS_ID).orElse(null);
        return settings;
    }

    /**
     * Checks if masked login is enabled.
     * Defaults to false if settings not found.
     * 
     * @return true if masked login is enabled
     */
    @Transactional(readOnly = true)
    public boolean isEnabled() {
        Optional<MaskedLoginSettings> settings = maskedLoginSettingsRepository.findById(SETTINGS_ID);
        return settings.map(MaskedLoginSettings::getEnabled)
                .orElse(false);
    }

    /**
     * Gets current template ID.
     * Defaults to 1 if settings not found.
     * 
     * @return Template ID (1-10)
     */
    @Transactional(readOnly = true)
    public Integer getTemplateId() {
        Optional<MaskedLoginSettings> settings = maskedLoginSettingsRepository.findById(SETTINGS_ID);
        return settings.map(MaskedLoginSettings::getTemplateId)
                .orElse(1);
    }

    /**
     * Updates masked login settings.
     * 
     * @param enabled Whether masked login is enabled
     * @param templateId Template ID (1-10)
     * @param updatedBy Email of the user who updated the settings
     */
    @Transactional
    public void updateSettings(Boolean enabled, Integer templateId, String updatedBy) {
        if (templateId < MIN_TEMPLATE_ID || templateId > MAX_TEMPLATE_ID) {
            throw new IllegalArgumentException(
                    String.format("Template ID must be between %d and %d", MIN_TEMPLATE_ID, MAX_TEMPLATE_ID));
        }

        MaskedLoginSettings settings = maskedLoginSettingsRepository.findById(SETTINGS_ID)
                .orElse(new MaskedLoginSettings());

        settings.setId(SETTINGS_ID);
        settings.setEnabled(enabled != null ? enabled : false);
        settings.setTemplateId(templateId);
        settings.setUpdatedAt(LocalDateTime.now());
        settings.setUpdatedBy(updatedBy);

        maskedLoginSettingsRepository.save(settings);
        log.info("Masked login settings updated: enabled={}, templateId={}, updatedBy={}", 
                enabled, templateId, updatedBy);
    }
}

