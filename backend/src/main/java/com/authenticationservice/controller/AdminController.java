// AdminController.java
package com.authenticationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.authenticationservice.constants.ApiConstants;
import com.authenticationservice.dto.AdminUpdateUserRequest;
import com.authenticationservice.dto.UserDTO;
import com.authenticationservice.service.AdminService; // Подключаем сервис

import java.util.List;

@RestController
@RequestMapping(ApiConstants.ADMIN_BASE_URL)
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @PostMapping(ApiConstants.WHITELIST_ADD_URL)
    public ResponseEntity<String> addToWhitelist(@RequestParam String email) {
        try {
            adminService.addToWhitelist(email);
            return ResponseEntity.ok("Добавлен в белый список: " + email);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping(ApiConstants.WHITELIST_REMOVE_URL)
    public ResponseEntity<String> removeFromWhitelist(@RequestParam String email) {
        try {
            adminService.removeFromWhitelist(email);
            return ResponseEntity.ok("Удалён из белого списка: " + email);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(ApiConstants.USERS_URL)
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping(ApiConstants.USER_ID_URL)
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminService.getUserById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build(); // Или badRequest, в зависимости от логики
        }
    }

    @PutMapping(ApiConstants.USER_ID_URL)
    public ResponseEntity<String> updateUser(@PathVariable Long id, @RequestBody AdminUpdateUserRequest request) {
        try {
            adminService.updateUser(id, request);
            return ResponseEntity.ok("Пользователь обновлён");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping(ApiConstants.USER_ID_URL)
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        try {
            adminService.deleteUser(id);
            return ResponseEntity.ok("Пользователь удалён");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(ApiConstants.WHITELIST_URL)
    public ResponseEntity<List<String>> getWhitelist() {
        return ResponseEntity.ok(adminService.getWhitelist());
    }
}