package com.authenticationservice.service;

import com.authenticationservice.config.AdminConfig;
import com.authenticationservice.constants.TestConstants;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.RoleRepository;
import com.authenticationservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    @InjectMocks
    private AdminInitializationService adminInitializationService;

    private User existingUser;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setName("existinguser");
        existingUser.setEmail(TestConstants.ADMIN_EMAIL);
        existingUser.setEnabled(true);

        adminRole = new Role();
        adminRole.setName(TestConstants.ROLE_ADMIN);
    }

    @Test
    void initializeAdmin_shouldSkipInitialization_whenAdminIsDisabled() {
        // Arrange
        when(adminConfig.isEnabled()).thenReturn(false);

        // Act
        adminInitializationService.initializeAdmin();

        // Assert
        verify(adminConfig).isEnabled();
        verifyNoMoreInteractions(adminConfig, userRepository, roleRepository, emailService, authService);
    }

    @Test
    void initializeAdmin_shouldSkipInitialization_whenNoEmailConfigured() {
        // Arrange
        when(adminConfig.isEnabled()).thenReturn(true);
        when(adminConfig.getEmail()).thenReturn(null);

        // Act
        adminInitializationService.initializeAdmin();

        // Assert
        verify(adminConfig).isEnabled();
        verify(adminConfig).getEmail();
        verifyNoMoreInteractions(adminConfig, userRepository, roleRepository, emailService, authService);
    }

    @Test
    void initializeAdmin_shouldAddAdminRole_whenUserExists() {
        // Arrange
        when(adminConfig.isEnabled()).thenReturn(true);
        when(adminConfig.getEmail()).thenReturn(TestConstants.ADMIN_EMAIL);
        when(userRepository.findByEmail(TestConstants.ADMIN_EMAIL)).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByName(TestConstants.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));

        // Act
        adminInitializationService.initializeAdmin();

        // Assert
        verify(adminConfig).isEnabled();
        verify(adminConfig).getEmail();
        verify(userRepository).findByEmail(TestConstants.ADMIN_EMAIL);
        verify(roleRepository).findByName(TestConstants.ROLE_ADMIN);
        verify(userRepository).save(any(User.class));
        verifyNoMoreInteractions(adminConfig, userRepository, roleRepository, emailService, authService);
    }

    @Test
    void initializeAdmin_shouldCreateNewAdmin_whenUserDoesNotExist() {
        // Arrange
        when(adminConfig.isEnabled()).thenReturn(true);
        when(adminConfig.getEmail()).thenReturn(TestConstants.ADMIN_EMAIL);
        when(adminConfig.getUsername()).thenReturn(TestConstants.ADMIN_USERNAME);
        when(userRepository.findByEmail(TestConstants.ADMIN_EMAIL)).thenReturn(Optional.empty());
        when(roleRepository.findByName(TestConstants.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(authService.generatePasswordResetToken(anyString())).thenReturn(TestConstants.RESET_TOKEN);

        // Act
        adminInitializationService.initializeAdmin();

        // Assert
        verify(adminConfig).isEnabled();
        verify(adminConfig).getEmail();
        verify(adminConfig).getUsername();
        verify(userRepository).findByEmail(TestConstants.ADMIN_EMAIL);
        verify(roleRepository).findByName(TestConstants.ROLE_ADMIN);
        verify(authService).generatePasswordResetToken(anyString());
        verify(userRepository).save(any(User.class));
        verify(emailService).sendEmail(anyString(), anyString(), anyString());
        verifyNoMoreInteractions(adminConfig, userRepository, roleRepository, emailService, authService);
    }
} 