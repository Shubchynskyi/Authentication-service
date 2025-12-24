package com.authenticationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.authenticationservice.dto.MaskedLoginPublicSettingsDTO;
import com.authenticationservice.model.MaskedLoginSettings;
import com.authenticationservice.service.MaskedLoginService;

/**
 * Controller for serving masked login templates.
 * Templates are stored as static resources.
 */
@RestController
@RequestMapping("/api/public/masked-login")
@RequiredArgsConstructor
public class MaskedLoginController {

    private final MaskedLoginService maskedLoginService;

    private static final String TEMPLATE_PATH_PREFIX = "templates/masked-login/";
    private static final int MIN_TEMPLATE_ID = 1;
    private static final int MAX_TEMPLATE_ID = 10;
    private static final Map<Integer, String> TEMPLATE_FILES = Map.ofEntries(
            Map.entry(1, "template_01_404.html"),
            Map.entry(2, "template_02_maintenance.html"),
            Map.entry(3, "template_03_cooking.html"),
            Map.entry(4, "template_04_terms.html"),
            Map.entry(5, "template_05_about.html"),
            Map.entry(6, "template_06_catfacts.html"),
            Map.entry(7, "template_07_lorem.html"),
            Map.entry(8, "template_08_weather.html"),
            Map.entry(9, "template_09_coming_soon.html"),
            Map.entry(10, "template_10_db_error.html")
    );

    @GetMapping("/settings")
    public ResponseEntity<MaskedLoginPublicSettingsDTO> getMaskedLoginSettings() {
        MaskedLoginSettings settings = maskedLoginService.getSettings();
        if (settings == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(new MaskedLoginPublicSettingsDTO(settings.getEnabled(), settings.getTemplateId()));
    }

    @GetMapping("/template/{templateId}")
    public ResponseEntity<String> getTemplate(@PathVariable Integer templateId) {
        if (templateId < MIN_TEMPLATE_ID || templateId > MAX_TEMPLATE_ID) {
            return ResponseEntity.badRequest()
                    .body("Template ID must be between " + MIN_TEMPLATE_ID + " and " + MAX_TEMPLATE_ID);
        }

        String fileName = TEMPLATE_FILES.get(templateId);
        if (fileName == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Template not found: " + templateId);
        }

        Resource resource = new ClassPathResource(TEMPLATE_PATH_PREFIX + fileName);

        if (!resource.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Template not found: " + templateId);
        }

        try {
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading template: " + e.getMessage());
        }
    }
}

