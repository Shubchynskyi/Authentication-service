package com.authenticationservice.config;

import com.authenticationservice.constants.TestConstants;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Utility class for configuring test properties dynamically.
 * Centralizes all test property configuration to avoid duplication.
 */
public final class TestPropertyConfigurator {
    
    private TestPropertyConfigurator() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Configures all test properties for integration tests.
     * 
     * @param registry DynamicPropertyRegistry to configure
     * @param postgres PostgreSQLContainer instance for database properties
     */
    public static void configureProperties(DynamicPropertyRegistry registry, PostgreSQLContainer postgres) {
        // Database properties
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // JWT properties
        registry.add("jwt.access-secret", () -> TestConstants.TestProperties.JWT_ACCESS_SECRET);
        registry.add("jwt.refresh-secret", () -> TestConstants.TestProperties.JWT_REFRESH_SECRET);
        registry.add("jwt.access-expiration", () -> TestConstants.TestProperties.JWT_ACCESS_EXPIRATION);
        registry.add("jwt.refresh-expiration", () -> TestConstants.TestProperties.JWT_REFRESH_EXPIRATION);
        
        // Mail properties
        registry.add("spring.mail.host", () -> TestConstants.TestProperties.MAIL_HOST);
        registry.add("spring.mail.port", () -> TestConstants.TestProperties.MAIL_PORT);
        registry.add("spring.mail.username", () -> TestConstants.TestProperties.MAIL_USERNAME);
        registry.add("spring.mail.password", () -> TestConstants.TestProperties.MAIL_PASSWORD);
        
        // Frontend properties
        registry.add("frontend.url", () -> TestConstants.TestProperties.FRONTEND_URL);
        
        // Admin properties
        registry.add("admin.enabled", () -> TestConstants.TestProperties.ADMIN_ENABLED);
        
        // OAuth properties
        registry.add("spring.security.oauth2.client.registration.google.client-id", 
                () -> TestConstants.TestProperties.OAUTH_GOOGLE_CLIENT_ID);
        registry.add("spring.security.oauth2.client.registration.google.client-secret", 
                () -> TestConstants.TestProperties.OAUTH_GOOGLE_CLIENT_SECRET);
        
        // Password validation - use default pattern for tests
        registry.add("password.validation.pattern", 
                () -> "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!\\-_*?])(?=\\S+$).{8,}$");
    }
}

