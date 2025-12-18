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

        @Mock
        private com.authenticationservice.util.EmailTemplateFactory emailTemplateFactory;

        @InjectMocks
        private AdminInitializationService adminInitializationService;

        private User existingUser;
        private Role adminRole;

        @BeforeEach
        void setUp() {
                // Create admin user and role for testing
                existingUser = createAdminUser();
                adminRole = createAdminRole();
                
                // Mock EmailTemplateFactory methods
                lenient().when(emailTemplateFactory.buildResetPasswordText(anyString())).thenReturn("Reset password text");
                lenient().when(emailTemplateFactory.buildResetPasswordHtml(anyString())).thenReturn("Reset password html");
        }
        
        /**
         * Creates an admin user for testing
         * 
         * @return User with admin role and default test data
         */
        private User createAdminUser() {
                User user = new User();
                user.setId(1L);
                user.setName(TestConstants.UserData.ADMIN_USERNAME);
                user.setEmail(TestConstants.UserData.ADMIN_EMAIL);
                user.setEnabled(true);
                user.getRoles().add(createAdminRole());
                return user;
        }
        
        /**
         * Creates an admin role for testing
         * 
         * @return Admin role with default test data
         */
        private Role createAdminRole() {
                Role role = new Role();
                role.setName(TestConstants.Roles.ROLE_ADMIN);
                return role;
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
                        
                        // Verify new admin user is saved
                        org.mockito.ArgumentCaptor<User> userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
                        verify(userRepository).save(userCaptor.capture());
                        
                        User savedUser = userCaptor.getValue();
                        assertEquals(TestConstants.UserData.ADMIN_EMAIL, savedUser.getEmail(), 
                                    "Admin email should match configured value");
                        assertTrue(savedUser.isEnabled(), "Admin user should be enabled");
                        assertEquals(TestConstants.UserData.ENCODED_PASSWORD, savedUser.getPassword(), 
                                    "Password should be encoded");
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
                                        () -> adminInitializationService.initializeAdmin(),
                                        "Should throw exception when admin role is not found");
                        assertEquals(TestConstants.ErrorMessages.ADMIN_ROLE_NOT_FOUND, ex.getMessage(),
                                    "Exception message should indicate admin role not found");
                        verify(userRepository).findByEmail(TestConstants.UserData.ADMIN_EMAIL);
                        verify(roleRepository).findByName(TestConstants.Roles.ROLE_ADMIN);
                        verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
                }
        }
}