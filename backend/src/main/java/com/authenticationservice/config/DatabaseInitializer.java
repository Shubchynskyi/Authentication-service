package com.authenticationservice.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.authenticationservice.model.Role;
import com.authenticationservice.repository.RoleRepository;

import java.util.Optional;

@Configuration
public class DatabaseInitializer {

    private final RoleRepository roleRepository;

    public DatabaseInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Bean
    CommandLineRunner initRoles() {
        return this::initializeRoles;
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