package com.authenticationservice.repository;

import com.authenticationservice.model.AccessMode;
import com.authenticationservice.model.AccessModeSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.authenticationservice.config.TestPropertyConfigurator;
import com.authenticationservice.constants.TestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@org.springframework.test.context.TestPropertySource(
    locations = "classpath:application-test.yml",
    properties = {
        "admin.enabled=false",
        "admin.email=admin@test.com",
        "admin.username=admin"
    }
)
@Import(com.authenticationservice.config.TestConfig.class)
@DisplayName("AccessModeSettingsRepository Tests")
class AccessModeSettingsRepositoryTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(TestConstants.TestDatabase.POSTGRES_IMAGE)
            .withDatabaseName(TestConstants.TestDatabase.DATABASE_NAME)
            .withUsername(TestConstants.TestDatabase.USERNAME)
            .withPassword(TestConstants.TestDatabase.PASSWORD);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        TestPropertyConfigurator.configureProperties(registry, postgres);
    }

    @Autowired
    private AccessModeSettingsRepository accessModeSettingsRepository;

    @Test
    @DisplayName("Should save and find access mode settings by id")
    void shouldSaveAndFindById() {
        // Arrange
        Long settingsId = 1L;
        AccessModeSettings settings = new AccessModeSettings();
        settings.setId(settingsId);
        settings.setMode(AccessMode.WHITELIST);

        // Act
        AccessModeSettings saved = accessModeSettingsRepository.save(settings);
        Optional<AccessModeSettings> found = accessModeSettingsRepository.findById(settingsId);

        // Assert
        assertNotNull(saved);
        assertTrue(found.isPresent());
        assertEquals(AccessMode.WHITELIST, found.get().getMode());
        assertEquals(settingsId, found.get().getId());
    }

    @Test
    @DisplayName("Should update access mode")
    void shouldUpdateAccessMode() {
        // Arrange
        Long settingsId = 1L;
        AccessModeSettings settings = new AccessModeSettings();
        settings.setId(settingsId);
        settings.setMode(AccessMode.WHITELIST);
        accessModeSettingsRepository.save(settings);

        // Act
        AccessModeSettings found = accessModeSettingsRepository.findById(settingsId).orElseThrow();
        found.setMode(AccessMode.BLACKLIST);
        AccessModeSettings updated = accessModeSettingsRepository.save(found);

        // Assert
        assertEquals(AccessMode.BLACKLIST, updated.getMode());
    }
}

