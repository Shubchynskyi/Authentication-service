// AdminService.java
package com.authenticationservice.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.authenticationservice.dto.AdminUpdateUserRequest;
import com.authenticationservice.dto.UserDTO;
import com.authenticationservice.model.AllowedEmail;
import com.authenticationservice.model.Role;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.AllowedEmailRepository;
import com.authenticationservice.repository.RoleRepository;
import com.authenticationservice.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final AllowedEmailRepository allowedEmailRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public void addToWhitelist(String email) {
        if (allowedEmailRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email уже в белом списке");
        }
        AllowedEmail allowedEmail = new AllowedEmail(email);
        allowedEmailRepository.save(allowedEmail);
    }

    public void removeFromWhitelist(String email) {
        AllowedEmail existing = allowedEmailRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email не найден в списке"));
        allowedEmailRepository.delete(existing);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserDTO::fromUser)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return UserDTO.fromUser(user);
    }

    @Transactional
    public void updateUser(Long id, AdminUpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getEmailVerified() != null) {
            user.setEmailVerified(request.getEmailVerified());
        }

        if (request.getRoles() != null) {
            Set<Role> newRoles = new HashSet<>();
            for (String roleName : request.getRoles().split(",")) {
                Role role = roleRepository.findByName(roleName.trim())
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName.trim()));
                newRoles.add(role);

            }
            user.setRoles(newRoles);
        }

        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<String> getWhitelist() {
        return allowedEmailRepository.findAll()
                .stream().map(AllowedEmail::getEmail)
                .toList();
    }
}