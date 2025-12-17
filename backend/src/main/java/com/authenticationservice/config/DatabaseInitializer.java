package com.authenticationservice.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import com.authenticationservice.model.Role;
import com.authenticationservice.repository.RoleRepository;
import com.authenticationservice.service.AdminInitializationService;
import com.authenticationservice.service.AccessModeInitializer;
import com.authenticationservice.service.MaskedLoginInitializer;

import java.util.Optional;

@Configuration
public class DatabaseInitializer {

    private final RoleRepository roleRepository;
    private final AdminInitializationService adminInitializationService;
    private final AccessModeInitializer accessModeInitializer;
    private final MaskedLoginInitializer maskedLoginInitializer;

    public DatabaseInitializer(RoleRepository roleRepository, AdminInitializationService adminInitializationService, AccessModeInitializer accessModeInitializer, MaskedLoginInitializer maskedLoginInitializer) {
        this.roleRepository = roleRepository;
        this.adminInitializationService = adminInitializationService;
        this.accessModeInitializer = accessModeInitializer;
        this.maskedLoginInitializer = maskedLoginInitializer;
    }

    @Bean
    @Order(1)
    CommandLineRunner initRoles() {
        return this::initializeRoles;
    }

    @Bean
    @Order(2)
    CommandLineRunner initAccessMode() {
        return args -> {
            accessModeInitializer.initialize();
        };
    }

    @Bean
    @Order(3)
    CommandLineRunner initMaskedLogin() {
        return args -> {
            maskedLoginInitializer.initialize();
        };
    }

    @Bean
    @Order(4)
    CommandLineRunner initAdmin() {
        return args -> {
            ensureRolesExist();
            adminInitializationService.initializeAdmin();
        };
    }

    private void ensureRolesExist() {
        createRoleIfNotExists("ROLE_USER");
        createRoleIfNotExists("ROLE_ADMIN");
    }

    private void initializeRoles(String... args) {
        createRoleIfNotExists("ROLE_USER");
        createRoleIfNotExists("ROLE_ADMIN");
    }

    private void createRoleIfNotExists(String roleName) {
        Optional<Role> roleOpt = roleRepository.findByName(roleName);
        if (roleOpt.isEmpty()) {
            Role role = new Role(roleName);
            roleRepository.save(role);
        }
    }
}