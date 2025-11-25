package com.authenticationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.authenticationservice.constants.ApiConstants;
import com.authenticationservice.constants.MessageConstants;
import com.authenticationservice.dto.ProfileUpdateRequest;
import com.authenticationservice.service.ProfileService;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.PROTECTED_BASE_URL)
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping(ApiConstants.PROFILE_URL)
    public ResponseEntity<?> getProfile(Principal principal) {
        try {
            return ResponseEntity.ok(profileService.getProfile(principal.getName()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(ApiConstants.PROFILE_URL)
    public ResponseEntity<String> updateProfile(@RequestBody ProfileUpdateRequest request, Principal principal) {
        try {
            profileService.updateProfile(principal.getName(), request);
            return ResponseEntity.ok(MessageConstants.PROFILE_UPDATED_SUCCESS);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(ApiConstants.CHECK_URL)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkAuthentication() {
        return ResponseEntity.ok().build();
    }
}