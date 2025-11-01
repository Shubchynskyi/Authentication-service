package com.authenticationservice.controller;

import com.authenticationservice.model.User;
import com.authenticationservice.repository.UserRepository;
import com.authenticationservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

// @RestController
// @RequestMapping("/api/v1/validate")
@RequiredArgsConstructor
public class TokenValidationController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token format"));
        }

        String token = authHeader.substring(7);
        
        if (!jwtTokenProvider.validateAccessToken(token)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));
        }

        String email = jwtTokenProvider.getEmailFromAccess(token);
        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("valid", true);
        response.put("user", Map.of(
            "email", user.getEmail(),
            "name", user.getName(),
            "enabled", user.isEnabled(),
            "blocked", user.isBlocked()
        ));
        response.put("roles", user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }
} 