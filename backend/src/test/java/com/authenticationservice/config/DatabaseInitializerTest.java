package com.authenticationservice.config;

import com.authenticationservice.constants.SecurityConstants;
import com.authenticationservice.model.Role;
import com.authenticationservice.repository.RoleRepository;
import com.authenticationservice.service.AdminInitializationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseInitializer Tests")
class DatabaseInitializerTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AdminInitializationService adminInitializationService;

    @InjectMocks
    private DatabaseInitializer databaseInitializer;

    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userRole = new Role(SecurityConstants.ROLE_USER);
        adminRole = new Role(SecurityConstants.ROLE_ADMIN);
    }

    @Test
    @DisplayName("Should create ROLE_USER when it does not exist")
    void initializeRoles_shouldCreateRoleUser_whenNotExists() {
        // Arrange
        when(roleRepository.findByName(SecurityConstants.ROLE_USER)).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        try {
            databaseInitializer.initRoles().run();
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }

        // Assert
        verify(roleRepository).findByName(SecurityConstants.ROLE_USER);
        verify(roleRepository).save(argThat(role -> 
            role.getName().equals(SecurityConstants.ROLE_USER)));
    }

    @Test
    @DisplayName("Should create ROLE_ADMIN when it does not exist")
    void initializeRoles_shouldCreateRoleAdmin_whenNotExists() {
        // Arrange
        when(roleRepository.findByName(SecurityConstants.ROLE_USER)).thenReturn(Optional.empty());
        when(roleRepository.findByName(SecurityConstants.ROLE_ADMIN)).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        try {
            databaseInitializer.initRoles().run();
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }

        // Assert
        verify(roleRepository).findByName(SecurityConstants.ROLE_USER);
        verify(roleRepository).findByName(SecurityConstants.ROLE_ADMIN);
        verify(roleRepository, atLeastOnce()).save(argThat(role -> 
            role.getName().equals(SecurityConstants.ROLE_ADMIN)));
    }

    @Test
    @DisplayName("Should not create ROLE_USER when it already exists")
    void initializeRoles_shouldNotCreateRoleUser_whenExists() {
        // Arrange
        when(roleRepository.findByName(SecurityConstants.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(roleRepository.findByName(SecurityConstants.ROLE_ADMIN)).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        try {
            databaseInitializer.initRoles().run();
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }

        // Assert
        verify(roleRepository).findByName(SecurityConstants.ROLE_USER);
        verify(roleRepository, never()).save(argThat(role -> 
            role.getName().equals(SecurityConstants.ROLE_USER)));
    }

    @Test
    @DisplayName("Should not create ROLE_ADMIN when it already exists")
    void initializeRoles_shouldNotCreateRoleAdmin_whenExists() {
        // Arrange
        when(roleRepository.findByName(SecurityConstants.ROLE_USER)).thenReturn(Optional.empty());
        when(roleRepository.findByName(SecurityConstants.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        try {
            databaseInitializer.initRoles().run();
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }

        // Assert
        verify(roleRepository).findByName(SecurityConstants.ROLE_ADMIN);
        verify(roleRepository, never()).save(argThat(role -> 
            role.getName().equals(SecurityConstants.ROLE_ADMIN)));
    }

    @Test
    @DisplayName("Should create both roles when they do not exist")
    void initializeRoles_shouldCreateBothRoles_whenNotExist() {
        // Arrange
        when(roleRepository.findByName(SecurityConstants.ROLE_USER)).thenReturn(Optional.empty());
        when(roleRepository.findByName(SecurityConstants.ROLE_ADMIN)).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        try {
            databaseInitializer.initRoles().run();
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }

        // Assert
        verify(roleRepository, times(2)).findByName(anyString());
        verify(roleRepository, times(2)).save(any(Role.class));
    }

    @Test
    @DisplayName("Should ensure roles exist before initializing admin")
    void initAdmin_shouldEnsureRolesExistBeforeInitializingAdmin() {
        // Arrange
        when(roleRepository.findByName(SecurityConstants.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(roleRepository.findByName(SecurityConstants.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        doNothing().when(adminInitializationService).initializeAdmin();

        // Act
        try {
            databaseInitializer.initAdmin().run();
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }

        // Assert
        verify(roleRepository).findByName(SecurityConstants.ROLE_USER);
        verify(roleRepository).findByName(SecurityConstants.ROLE_ADMIN);
        verify(adminInitializationService).initializeAdmin();
    }

    @Test
    @DisplayName("Should create missing roles before initializing admin")
    void initAdmin_shouldCreateMissingRolesBeforeInitializingAdmin() {
        // Arrange
        when(roleRepository.findByName(SecurityConstants.ROLE_USER)).thenReturn(Optional.empty());
        when(roleRepository.findByName(SecurityConstants.ROLE_ADMIN)).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(adminInitializationService).initializeAdmin();

        // Act
        try {
            databaseInitializer.initAdmin().run();
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }

        // Assert
        verify(roleRepository, times(2)).findByName(anyString());
        verify(roleRepository, times(2)).save(any(Role.class));
        verify(adminInitializationService).initializeAdmin();
    }

    @Test
    @DisplayName("Should create only missing roles before initializing admin")
    void initAdmin_shouldCreateOnlyMissingRolesBeforeInitializingAdmin() {
        // Arrange
        when(roleRepository.findByName(SecurityConstants.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(roleRepository.findByName(SecurityConstants.ROLE_ADMIN)).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(adminInitializationService).initializeAdmin();

        // Act
        try {
            databaseInitializer.initAdmin().run();
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }

        // Assert
        verify(roleRepository).findByName(SecurityConstants.ROLE_USER);
        verify(roleRepository).findByName(SecurityConstants.ROLE_ADMIN);
        verify(roleRepository, times(1)).save(any(Role.class));
        verify(adminInitializationService).initializeAdmin();
    }
}

