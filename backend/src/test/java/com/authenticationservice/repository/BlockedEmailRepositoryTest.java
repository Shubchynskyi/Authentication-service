package com.authenticationservice.repository;

import com.authenticationservice.config.TestPropertyConfigurator;
import com.authenticationservice.constants.TestConstants;
import com.authenticationservice.model.BlockedEmail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
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
@DisplayName("BlockedEmailRepository Tests")
class BlockedEmailRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(TestConstants.TestDatabase.POSTGRES_IMAGE)
            .withDatabaseName(TestConstants.TestDatabase.DATABASE_NAME)
            .withUsername(TestConstants.TestDatabase.USERNAME)
            .withPassword(TestConstants.TestDatabase.PASSWORD);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        TestPropertyConfigurator.configureProperties(registry, postgres);
    }

    @Autowired
    private BlockedEmailRepository blockedEmailRepository;

    @Test
    @DisplayName("Should save and find blocked email by email")
    void shouldSaveAndFindByEmail() {
        // Arrange
        String email = "blocked@example.com";
        BlockedEmail blockedEmail = new BlockedEmail(email);

        // Act
        BlockedEmail saved = blockedEmailRepository.save(blockedEmail);
        Optional<BlockedEmail> found = blockedEmailRepository.findByEmail(email);

        // Assert
        assertNotNull(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(email, found.get().getEmail());
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void shouldReturnEmptyWhenEmailNotFound() {
        // Act
        Optional<BlockedEmail> found = blockedEmailRepository.findByEmail("notfound@example.com");

        // Assert
        assertTrue(found.isEmpty());
    }
}

