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

import jakarta.validation.Valid;
import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.PROTECTED_BASE_URL)
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping(ApiConstants.PROFILE_URL)
    public ResponseEntity<?> getProfile(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(profileService.getProfile(principal.getName()));
    }

    @PostMapping(ApiConstants.PROFILE_URL)
    public ResponseEntity<String> updateProfile(@Valid @RequestBody ProfileUpdateRequest request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        profileService.updateProfile(principal.getName(), request);
        return ResponseEntity.ok(MessageConstants.PROFILE_UPDATED_SUCCESS);
    }

    @GetMapping(ApiConstants.CHECK_URL)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkAuthentication() {
        return ResponseEntity.ok().build();
    }
}