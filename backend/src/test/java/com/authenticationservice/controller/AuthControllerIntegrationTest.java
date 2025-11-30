package com.authenticationservice.controller;

import com.authenticationservice.config.TestPropertyConfigurator;
import com.authenticationservice.constants.ApiConstants;
import com.authenticationservice.constants.SecurityConstants;
import com.authenticationservice.constants.TestConstants;
import com.authenticationservice.dto.LoginRequest;
import com.authenticationservice.dto.RegistrationRequest;
import com.authenticationservice.model.AllowedEmail;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.AllowedEmailRepository;
import com.authenticationservice.repository.RoleRepository;
import com.authenticationservice.repository.UserRepository;
import com.authenticationservice.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
@Transactional
@org.springframework.test.context.TestPropertySource(locations = "classpath:application-test.yml")
@Import(com.authenticationservice.config.TestConfig.class)
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

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
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AllowedEmailRepository allowedEmailRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    private User testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        // Use TransactionTemplate to explicitly commit transaction
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute(status -> {
            // Clean up
            userRepository.deleteAll();
            userRepository.flush();
            allowedEmailRepository.deleteAll();
            allowedEmailRepository.flush();
            // Don't delete roles - DatabaseInitializer creates them on startup
            // Find existing roles or create if they don't exist
            userRole = roleRepository.findByName(SecurityConstants.ROLE_USER)
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName(SecurityConstants.ROLE_USER);
                        return roleRepository.save(role);
                    });

            roleRepository.findByName(SecurityConstants.ROLE_ADMIN)
                    .orElseGet(() -> {
                        Role adminRole = new Role();
                        adminRole.setName(SecurityConstants.ROLE_ADMIN);
                        return roleRepository.save(adminRole);
                    });

            // Create allowed email
            AllowedEmail allowedEmail = new AllowedEmail();
            allowedEmail.setEmail(TestConstants.UserData.TEST_EMAIL);
            allowedEmailRepository.save(allowedEmail);

            // Create test user
            testUser = new User();
            testUser.setEmail(TestConstants.UserData.TEST_EMAIL);
            testUser.setName(TestConstants.UserData.TEST_USERNAME);
            testUser.setPassword(passwordEncoder.encode(TestConstants.UserData.TEST_PASSWORD));
            testUser.setEnabled(true);
            testUser.setBlocked(false);
            testUser.setEmailVerified(true);
            testUser.setLockTime(null);
            Set<Role> userRoles = new HashSet<>();
            userRoles.add(userRole);
            testUser.setRoles(userRoles);
            userRepository.save(testUser);
            // Force flush to ensure user is persisted
            userRepository.flush();
            return null;
        });
        
        // Verify user is actually in database after commit
        User verifyUser = transactionTemplate.execute(status -> 
            userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL)
                .orElseThrow(() -> new RuntimeException("User was not saved to database in setUp!"))
        );
        if (verifyUser != null) {
            System.out.println("DEBUG: User verified in database after setUp: " + verifyUser.getEmail());
        }
    }

    @Test
    @DisplayName("Should register user successfully")
    void register_shouldRegisterUserSuccessfully() throws Exception {
        // Arrange
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail(TestConstants.TestData.NEW_USER_EMAIL);
        request.setName(TestConstants.TestData.NEW_USER_NAME);
        request.setPassword(TestConstants.TestData.NEW_USER_PASSWORD);

        // Use TransactionTemplate to explicitly commit transaction
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute(status -> {
            AllowedEmail allowedEmail = new AllowedEmail();
            allowedEmail.setEmail(TestConstants.TestData.NEW_USER_EMAIL);
            allowedEmailRepository.save(allowedEmail);
            allowedEmailRepository.flush();
            return null;
        });

        // Act & Assert
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Check your email")));
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void login_shouldLoginSuccessfully() throws Exception {
        // Arrange - verify user exists and is correctly saved in a new transaction
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        User savedUser = transactionTemplate.execute(status -> {
            User user = userRepository.findByEmail(TestConstants.UserData.TEST_EMAIL)
                    .orElseThrow(() -> new RuntimeException("User not found in database"));
            System.out.println("DEBUG: Found user in new transaction: " + user.getEmail());
            System.out.println("DEBUG: User enabled: " + user.isEnabled());
            System.out.println("DEBUG: User verified: " + user.isEmailVerified());
            System.out.println("DEBUG: User blocked: " + user.isBlocked());
            return user;
        });
        
        assertNotNull(savedUser, "User should not be null");
        assertTrue(savedUser.isEmailVerified(), "User email should be verified");
        assertTrue(savedUser.isEnabled(), "User should be enabled");
        assertFalse(savedUser.isBlocked(), "User should not be blocked");
        assertTrue(passwordEncoder.matches(TestConstants.UserData.TEST_PASSWORD, savedUser.getPassword()),
                "Password should match");

        LoginRequest request = new LoginRequest();
        request.setEmail(TestConstants.UserData.TEST_EMAIL);
        request.setPassword(TestConstants.UserData.TEST_PASSWORD);

        // Act & Assert
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(result -> {
                    // Print response for debugging
                    System.out.println("DEBUG: Response status: " + result.getResponse().getStatus());
                    System.out.println("DEBUG: Response body: " + result.getResponse().getContentAsString());
                    if (result.getResponse().getStatus() != 200) {
                        System.out.println("ERROR: Expected 200 but got " + result.getResponse().getStatus());
                    }
                })
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    @DisplayName("Should return unauthorized with invalid credentials")
    void login_shouldReturnUnauthorizedWithInvalidCredentials() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail(TestConstants.UserData.TEST_EMAIL);
        request.setPassword("wrongpassword");

        // Act & Assert
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void refresh_shouldRefreshTokenSuccessfully() throws Exception {
        // Arrange
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);

        // Act & Assert
        mockMvc.perform(post(ApiConstants.AUTH_BASE_URL + ApiConstants.REFRESH_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }
}
