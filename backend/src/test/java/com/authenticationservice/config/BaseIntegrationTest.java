package com.authenticationservice.config;

import com.authenticationservice.constants.SecurityConstants;
import com.authenticationservice.constants.TestConstants;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.AllowedEmailRepository;
import com.authenticationservice.repository.AccessModeSettingsRepository;
import com.authenticationservice.repository.RoleRepository;
import com.authenticationservice.repository.UserRepository;
import com.authenticationservice.security.JwtTokenProvider;
import com.authenticationservice.model.AccessMode;
import com.authenticationservice.model.AccessModeSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.Set;

/**
 * Base class for integration tests.
 * Provides common setup for Testcontainers, database initialization,
 * and helper methods for creating test users and roles.
 */
@Testcontainers
public abstract class BaseIntegrationTest {

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
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected AllowedEmailRepository allowedEmailRepository;

    @Autowired
    protected AccessModeSettingsRepository accessModeSettingsRepository;

    @Autowired
    protected BCryptPasswordEncoder passwordEncoder;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @Autowired
    protected org.springframework.transaction.PlatformTransactionManager transactionManager;

    /**
     * Creates or retrieves a role by name.
     * 
     * @param roleName Name of the role
     * @return Role entity
     */
    protected Role getOrCreateRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(roleName);
                    return roleRepository.save(role);
                });
    }

    /**
     * Creates a test user with specified parameters.
     * 
     * @param email User email
     * @param name Username
     * @param password User password (will be encoded)
     * @param enabled Whether user is enabled
     * @param blocked Whether user is blocked
     * @param emailVerified Whether email is verified
     * @param roles Set of role names
     * @return Created User entity
     */
    protected User createTestUser(String email, String name, String password,
                                  boolean enabled, boolean blocked, boolean emailVerified,
                                  Set<String> roles) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(enabled);
        user.setBlocked(blocked);
        user.setEmailVerified(emailVerified);
        user.setLockTime(null);
        user.setFailedLoginAttempts(0);

        Set<Role> userRoles = new HashSet<>();
        for (String roleName : roles) {
            userRoles.add(getOrCreateRole(roleName));
        }
        user.setRoles(userRoles);

        return userRepository.save(user);
    }

    /**
     * Creates a default test user with USER role.
     * 
     * @return Created User entity
     */
    protected User createDefaultTestUser() {
        Set<String> roles = new HashSet<>();
        roles.add(SecurityConstants.ROLE_USER);
        return createTestUser(
                TestConstants.UserData.TEST_EMAIL,
                TestConstants.UserData.TEST_USERNAME,
                TestConstants.UserData.TEST_PASSWORD,
                true, false, true, roles
        );
    }

    /**
     * Creates an admin user with ADMIN and USER roles.
     * 
     * @return Created User entity
     */
    protected User createAdminUser() {
        Set<String> roles = new HashSet<>();
        roles.add(SecurityConstants.ROLE_ADMIN);
        roles.add(SecurityConstants.ROLE_USER);
        return createTestUser(
                TestConstants.UserData.ADMIN_EMAIL,
                TestConstants.TestData.ADMIN_NAME,
                TestConstants.TestData.ADMIN_PASSWORD,
                true, false, true, roles
        );
    }

    /**
     * Generates an access token for a user.
     * 
     * @param user User entity
     * @return JWT access token
     */
    protected String generateAccessToken(User user) {
        return jwtTokenProvider.generateAccessToken(user);
    }

    /**
     * Generates a refresh token for a user.
     * 
     * @param user User entity
     * @return JWT refresh token
     */
    protected String generateRefreshToken(User user) {
        return jwtTokenProvider.generateRefreshToken(user);
    }

    /**
     * Gets a TransactionTemplate for explicit transaction management.
     * 
     * @return TransactionTemplate instance
     */
    protected TransactionTemplate getTransactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    /**
     * Cleans up test data (users and allowed emails).
     * Roles are not deleted as they are managed by DatabaseInitializer.
     */
    protected void cleanupTestData() {
        TransactionTemplate transactionTemplate = getTransactionTemplate();
        transactionTemplate.execute(status -> {
            userRepository.deleteAll();
            userRepository.flush();
            allowedEmailRepository.deleteAll();
            allowedEmailRepository.flush();
            return null;
        });
    }

    /**
     * Ensures roles exist in database.
     * Creates ROLE_USER and ROLE_ADMIN if they don't exist.
     */
    protected void ensureRolesExist() {
        getOrCreateRole(SecurityConstants.ROLE_USER);
        getOrCreateRole(SecurityConstants.ROLE_ADMIN);
    }

    /**
     * Ensures AccessModeSettings exists and sets the provided mode.
     *
     * @param mode access mode to set
     * @return persisted AccessModeSettings
     */
    protected AccessModeSettings ensureAccessModeSettings(AccessMode mode) {
        TransactionTemplate transactionTemplate = getTransactionTemplate();
        return transactionTemplate.execute(status -> {
            AccessModeSettings settings = accessModeSettingsRepository.findById(1L)
                    .orElseGet(() -> {
                        AccessModeSettings s = new AccessModeSettings();
                        s.setId(1L);
                        return s;
                    });
            settings.setMode(mode);
            return accessModeSettingsRepository.saveAndFlush(settings);
        });
    }

    /**
     * Creates an allowed email in the whitelist.
     * 
     * @param email Email address to add to whitelist
     * @return Created AllowedEmail entity
     */
    protected com.authenticationservice.model.AllowedEmail createAllowedEmail(String email) {
        com.authenticationservice.model.AllowedEmail allowedEmail = new com.authenticationservice.model.AllowedEmail();
        allowedEmail.setEmail(email);
        return allowedEmailRepository.save(allowedEmail);
    }

    /**
     * Creates a test user with default values and saves it in a transaction.
     * 
     * @return Created User entity
     */
    protected User createAndSaveDefaultTestUser() {
        TransactionTemplate transactionTemplate = getTransactionTemplate();
        return transactionTemplate.execute(status -> {
            ensureRolesExist();
            User user = createDefaultTestUser();
            userRepository.flush();
            return user;
        });
    }

    /**
     * Creates an admin user and saves it in a transaction.
     * 
     * @return Created User entity
     */
    protected User createAndSaveAdminUser() {
        TransactionTemplate transactionTemplate = getTransactionTemplate();
        return transactionTemplate.execute(status -> {
            ensureRolesExist();
            User user = createAdminUser();
            userRepository.flush();
            return user;
        });
    }

    /**
     * Sets up test environment with roles and optionally default user.
     * 
     * @param createDefaultUser Whether to create default test user
     * @return Created user if createDefaultUser is true, null otherwise
     */
    protected User setupTestEnvironment(boolean createDefaultUser) {
        TransactionTemplate transactionTemplate = getTransactionTemplate();
        return transactionTemplate.execute(status -> {
            ensureRolesExist();
            if (createDefaultUser) {
                User user = createDefaultTestUser();
                userRepository.flush();
                return user;
            }
            return null;
        });
    }
}

