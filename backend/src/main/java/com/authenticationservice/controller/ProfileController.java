package com.authenticationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.authenticationservice.constants.ApiConstants;
import com.authenticationservice.constants.MessageConstants;
import com.authenticationservice.dto.ProfileUpdateRequest;
import com.authenticationservice.service.ProfileService;
import com.authenticationservice.util.LoggingSanitizer;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.Valid;
import java.security.Principal;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.PROTECTED_BASE_URL)
public class ProfileController {

    private final ProfileService profileService;

    private String maskEmail(String email) {
        return LoggingSanitizer.maskEmail(email);
    }

    @GetMapping(ApiConstants.PROFILE_URL)
    public ResponseEntity<?> getProfile(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.debug("Get profile request received for user: {}", maskEmail(principal.getName()));
        var profile = profileService.getProfile(principal.getName());
        log.debug("Profile retrieved successfully for user: {}", maskEmail(principal.getName()));
        return ResponseEntity.ok(profile);
    }

    @PostMapping(ApiConstants.PROFILE_URL)
    public ResponseEntity<String> updateProfile(@Valid @RequestBody ProfileUpdateRequest request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.debug("Update profile request received for user: {}", maskEmail(principal.getName()));
        profileService.updateProfile(principal.getName(), request);
        log.info("Profile updated successfully for user: {}", maskEmail(principal.getName()));
        return ResponseEntity.ok(MessageConstants.PROFILE_UPDATED_SUCCESS);
    }

    @GetMapping(ApiConstants.CHECK_URL)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkAuthentication() {
        return ResponseEntity.ok().build();
    }
}