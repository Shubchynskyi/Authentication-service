// AdminController.java
package com.authenticationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.authenticationservice.constants.ApiConstants;
import com.authenticationservice.dto.AdminUpdateUserRequest;
import com.authenticationservice.dto.UserDTO;
import com.authenticationservice.model.Role;
import com.authenticationservice.service.AdminService; // Подключаем сервис
import com.authenticationservice.repository.RoleRepository;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.security.Principal;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping(ApiConstants.ADMIN_BASE_URL)
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final RoleRepository roleRepository;

    @GetMapping("/roles")
    public ResponseEntity<List<String>> getAllRoles() {
        return ResponseEntity.ok(
            roleRepository.findAll().stream()
                .map(Role::getName)
                .collect(Collectors.toList())
        );
    }

    @GetMapping(ApiConstants.USERS_URL)
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminService.getAllUsers(PageRequest.of(page, size), search));
    }

    @PostMapping(ApiConstants.USERS_URL)
    public ResponseEntity<String> createUser(@RequestBody AdminUpdateUserRequest request) {
        try {
            adminService.createUser(request);
            return ResponseEntity.ok("Пользователь создан");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(ApiConstants.USER_ID_URL)
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminService.getUserById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping(ApiConstants.USER_ID_URL)
    public ResponseEntity<String> updateUser(@PathVariable Long id, @RequestBody AdminUpdateUserRequest request) {
        try {
            adminService.updateUser(id, request);
            return ResponseEntity.ok("Пользователь обновлен");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping(ApiConstants.USER_ID_URL)
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        try {
            adminService.deleteUser(id);
            return ResponseEntity.ok("Пользователь удален");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(ApiConstants.WHITELIST_URL)
    public ResponseEntity<List<String>> getWhitelist() {
        return ResponseEntity.ok(adminService.getWhitelist());
    }

    @PostMapping(ApiConstants.WHITELIST_ADD_URL)
    public ResponseEntity<String> addToWhitelist(@RequestParam String email) {
        try {
            adminService.addToWhitelist(email);
            return ResponseEntity.ok("Email добавлен в белый список");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping(ApiConstants.WHITELIST_REMOVE_URL)
    public ResponseEntity<String> removeFromWhitelist(@RequestParam String email) {
        try {
            adminService.removeFromWhitelist(email);
            return ResponseEntity.ok("Email удален из белого списка");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/verify-admin")
    public ResponseEntity<String> verifyAdmin(@RequestBody Map<String, String> request, Principal principal) {
        try {
            String password = request.get("password");
            if (password == null || password.isBlank()) {
                return ResponseEntity.badRequest().body("Пароль обязателен");
            }
            
            boolean verified = adminService.verifyAdminPassword(principal.getName(), password);
            if (!verified) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Неверный пароль");
            }
            
            return ResponseEntity.ok("Пароль подтвержден");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}