package com.authenticationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.authenticationservice.dto.ProfileUpdateRequest;
import com.authenticationservice.service.ProfileService;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/protected")
public class ProfileController {

    private final ProfileService profileService; // Используем сервис

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Principal principal) {
        try {
            return ResponseEntity.ok(profileService.getProfile(principal.getName())); // Используем principal
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // Обработка ошибок
        }
    }

    @PostMapping("/profile")
    public ResponseEntity<String> updateProfile(@RequestBody ProfileUpdateRequest request, Principal principal) {
        try {
            profileService.updateProfile(principal.getName(), request);
            return ResponseEntity.ok("Профиль успешно обновлен");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // Возвращаем сообщение об ошибке
        }
    }

    @GetMapping("/check")
    @PreAuthorize("isAuthenticated()") // ВАЖНО: Добавляем авторизацию
    public ResponseEntity<?> checkAuthentication() {
        // Если дошли до сюда, значит, пользователь аутентифицирован.
        // Можно вернуть что угодно (даже пустой ответ). Главное - статус 200 OK.
        return ResponseEntity.ok().build();
    }

}