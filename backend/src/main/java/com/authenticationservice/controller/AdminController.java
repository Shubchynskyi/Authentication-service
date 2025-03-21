// AdminController.java
package com.authenticationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.authenticationservice.dto.AdminUpdateUserRequest;
import com.authenticationservice.dto.UserDTO;
import com.authenticationservice.service.AdminService; // Подключаем сервис

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // ВАЖНО: Защищаем все методы контроллера
public class AdminController {

    private final AdminService adminService; // Внедряем сервис

    // Добавление email в белый список
    @PostMapping("/whitelist/add")
    public ResponseEntity<String> addToWhitelist(@RequestParam String email) {
        try {
            adminService.addToWhitelist(email);
            return ResponseEntity.ok("Добавлен в белый список: " + email);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Удаление email из белого списка
    @DeleteMapping("/whitelist/remove")
    public ResponseEntity<String> removeFromWhitelist(@RequestParam String email) {
        try {
            adminService.removeFromWhitelist(email);
            return ResponseEntity.ok("Удалён из белого списка: " + email);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Список пользователей
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    // Получить одного пользователя
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminService.getUserById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build(); // Или badRequest, в зависимости от логики
        }
    }

    // Изменить поля пользователя
    @PutMapping("/users/{id}")
    public ResponseEntity<String> updateUser(@PathVariable Long id, @RequestBody AdminUpdateUserRequest request) {
        try {
            adminService.updateUser(id, request);
            return ResponseEntity.ok("Пользователь обновлён");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Удалить пользователя
    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        try {
            adminService.deleteUser(id);
            return ResponseEntity.ok("Пользователь удалён");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }

    @GetMapping("/whitelist")
    public ResponseEntity<List<String>> getWhitelist() {
        return ResponseEntity.ok(adminService.getWhitelist());
    }
}