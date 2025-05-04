package com.authenticationservice.service;

import com.authenticationservice.config.AdminConfig;
import com.authenticationservice.constants.TestConstants;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.RoleRepository;
import com.authenticationservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminInitializationService Tests")
class AdminInitializationServiceTest {

        @Mock
        private AdminConfig adminConfig;

        @Mock
        private UserRepository userRepository;

        @Mock
        private RoleRepository roleRepository;

        @Mock
        private EmailService emailService;

        @Mock
        private AuthService authService;

        @Mock
        private BCryptPasswordEncoder passwordEncoder;

        @InjectMocks
        private AdminInitializationService adminInitializationService;

        private User existingUser;
        private Role adminRole;

        @BeforeEach
        void setUp() {
                existingUser = new User();
                adminRole = new Role();
                adminRole.setName(TestConstants.Roles.ROLE_ADMIN);

                existingUser.setId(1L);
                existingUser.setName("existinguser");
                existingUser.setEmail(TestConstants.UserData.ADMIN_EMAIL);
                existingUser.setEnabled(true);
                existingUser.getRoles().add(adminRole);

        }

        @Nested
        @DisplayName("Admin Initialization Tests")
        class AdminInitializationTests {
                @Test
                @DisplayName("Should skip initialization when admin is disabled")
                void initializeAdmin_shouldSkipInitialization_whenAdminIsDisabled() {
                        // Arrange
                        when(adminConfig.isEnabled()).thenReturn(false);

                        // Act
                        adminInitializationService.initializeAdmin();

                        // Assert
                        verify(adminConfig).isEnabled();
                        verifyNoMoreInteractions(adminConfig, userRepository, roleRepository, emailService,
                                        authService);
                }

                @Test
                @DisplayName("Should skip initialization when admin already exists")
                void initializeAdmin_shouldSkipInitialization_whenAdminExists() {
                        // Arrange
                        when(adminConfig.getEmail()).thenReturn(TestConstants.UserData.ADMIN_EMAIL);
                        when(adminConfig.isEnabled()).thenReturn(true);
                        when(userRepository.findByEmail(TestConstants.UserData.ADMIN_EMAIL))
                                        .thenReturn(Optional.of(existingUser));

                        // Act
                        adminInitializationService.initializeAdmin();

                        // Assert
                        verify(userRepository).findByEmail(TestConstants.UserData.ADMIN_EMAIL);
                        verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
                }

                @Test
                @DisplayName("Should create admin when admin does not exist")
                void initializeAdmin_shouldCreateAdmin_whenAdminNotExists() {
                        // Arrange
                        when(adminConfig.isEnabled()).thenReturn(true);
                        when(userRepository.findByEmail(TestConstants.UserData.ADMIN_EMAIL))
                                        .thenReturn(Optional.empty());
                        when(roleRepository.findByName(TestConstants.Roles.ROLE_ADMIN))
                                        .thenReturn(Optional.of(adminRole));
                        when(passwordEncoder.encode(anyString()))
                                        .thenReturn(TestConstants.UserData.ENCODED_PASSWORD);
                        when(userRepository.save(any(User.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));
                        when(adminConfig.getEmail()).thenReturn(TestConstants.UserData.ADMIN_EMAIL);

                        // Act
                        adminInitializationService.initializeAdmin();

                        // Assert
                        verify(userRepository).findByEmail(TestConstants.UserData.ADMIN_EMAIL);
                        verify(roleRepository).findByName(TestConstants.Roles.ROLE_ADMIN);
                        verify(passwordEncoder).encode(anyString());
                        verify(userRepository).save(any(User.class));
                }

                @Test
                @DisplayName("Should throw exception when admin role not found")
                void initializeAdmin_shouldThrowException_whenAdminRoleNotFound() {
                        // Arrange
                        when(adminConfig.isEnabled()).thenReturn(true);
                        when(adminConfig.getEmail()).thenReturn(TestConstants.UserData.ADMIN_EMAIL);
                        when(userRepository.findByEmail(TestConstants.UserData.ADMIN_EMAIL))
                                        .thenReturn(Optional.empty());
                        when(roleRepository.findByName(TestConstants.Roles.ROLE_ADMIN))
                                        .thenReturn(Optional.empty());

                        // Act & Assert
                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> adminInitializationService.initializeAdmin());
                        assertEquals(TestConstants.ErrorMessages.ADMIN_ROLE_NOT_FOUND, ex.getMessage());
                        verify(userRepository).findByEmail(TestConstants.UserData.ADMIN_EMAIL);
                        verify(roleRepository).findByName(TestConstants.Roles.ROLE_ADMIN);
                        verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
                }
        }
}