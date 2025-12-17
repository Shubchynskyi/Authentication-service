package com.authenticationservice.service;

import com.authenticationservice.model.MaskedLoginSettings;
import com.authenticationservice.repository.MaskedLoginSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initializes masked login settings on application startup.
 * Creates default settings (enabled=false, templateId=1) if no settings exist.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaskedLoginInitializer {

    private final MaskedLoginSettingsRepository maskedLoginSettingsRepository;

    @Transactional
    public void initialize() {
        log.info("Initializing masked login settings...");
        
        Long settingsId = 1L;
        MaskedLoginSettings existingSettings = maskedLoginSettingsRepository.findById(settingsId).orElse(null);
        
        if (existingSettings == null) {
            MaskedLoginSettings newSettings = new MaskedLoginSettings();
            newSettings.setId(settingsId);
            newSettings.setEnabled(false);
            newSettings.setTemplateId(1);
            maskedLoginSettingsRepository.save(newSettings);
            log.info("Created default masked login settings with enabled=false, templateId=1");
        } else {
            log.info("Masked login settings already exist with enabled={}, templateId={}", 
                    existingSettings.getEnabled(), existingSettings.getTemplateId());
        }
    }
}

