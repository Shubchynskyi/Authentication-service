// AdminController.java
package com.authenticationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.authenticationservice.constants.ApiConstants;
import com.authenticationservice.constants.MessageConstants;
import com.authenticationservice.constants.SecurityConstants;
import com.authenticationservice.dto.AdminUpdateUserRequest;
import com.authenticationservice.dto.UserDTO;
import com.authenticationservice.dto.UpdateUserRolesRequest;
import com.authenticationservice.model.AccessMode;
import com.authenticationservice.model.AccessModeSettings;
import com.authenticationservice.model.Role;
import com.authenticationservice.service.AdminService;
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

    @GetMapping(ApiConstants.ROLES_URL)
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
        // Exception handling is done via GlobalExceptionHandler
        // Note: createUser automatically adds to whitelist and removes from blacklist
        adminService.createUser(request);
        return ResponseEntity.ok(MessageConstants.USER_CREATED);
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
        // Exception handling is done via GlobalExceptionHandler
        adminService.updateUser(id, request);
        return ResponseEntity.ok(MessageConstants.USER_UPDATED);
    }

    @DeleteMapping(ApiConstants.USER_ID_URL)
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        // Exception handling is done via GlobalExceptionHandler
        adminService.deleteUser(id);
        return ResponseEntity.ok(MessageConstants.USER_DELETED);
    }

    @PutMapping(ApiConstants.USERS_ID_ROLES_URL)
    public ResponseEntity<?> updateUserRoles(@PathVariable Long id, @RequestBody UpdateUserRolesRequest request) {
        // Exception handling is done via GlobalExceptionHandler
        UserDTO updated = adminService.updateUserRoles(id, request.getRoles());
        return ResponseEntity.ok(updated);
    }

    @GetMapping(ApiConstants.WHITELIST_URL)
    public ResponseEntity<List<String>> getWhitelist() {
        return ResponseEntity.ok(adminService.getWhitelist());
    }

    @PostMapping(ApiConstants.WHITELIST_ADD_URL)
    public ResponseEntity<String> addToWhitelist(
            @RequestParam String email,
            @RequestParam(required = false) String reason) {
        // Exception handling is done via GlobalExceptionHandler
        adminService.addToWhitelist(email, reason != null ? reason : "");
        return ResponseEntity.ok(MessageConstants.EMAIL_ADDED_TO_WHITELIST);
    }

    @DeleteMapping(ApiConstants.WHITELIST_REMOVE_URL)
    public ResponseEntity<String> removeFromWhitelist(
            @RequestParam String email,
            @RequestParam(required = false) String reason) {
        // Exception handling is done via GlobalExceptionHandler
        adminService.removeFromWhitelist(email, reason != null ? reason : "");
        return ResponseEntity.ok(MessageConstants.EMAIL_REMOVED_FROM_WHITELIST);
    }

    @GetMapping("/blacklist")
    public ResponseEntity<List<String>> getBlacklist() {
        return ResponseEntity.ok(adminService.getBlacklist());
    }

    @PostMapping("/blacklist/add")
    public ResponseEntity<String> addToBlacklist(
            @RequestParam String email,
            @RequestParam(required = false) String reason) {
        // Exception handling is done via GlobalExceptionHandler
        adminService.addToBlacklist(email, reason != null ? reason : "");
        return ResponseEntity.ok("Email added to blacklist");
    }

    @DeleteMapping("/blacklist/remove")
    public ResponseEntity<String> removeFromBlacklist(
            @RequestParam String email,
            @RequestParam(required = false) String reason) {
        // Exception handling is done via GlobalExceptionHandler
        adminService.removeFromBlacklist(email, reason != null ? reason : "");
        return ResponseEntity.ok("Email removed from blacklist");
    }

    @GetMapping("/access-mode")
    public ResponseEntity<AccessModeSettings> getAccessMode() {
        return ResponseEntity.ok(adminService.getAccessModeSettings());
    }

    @PostMapping("/access-mode/request-otp")
    public ResponseEntity<String> requestModeChangeOtp(Principal principal) {
        // Exception handling is done via GlobalExceptionHandler
        adminService.sendModeChangeOtp(principal.getName());
        return ResponseEntity.ok("OTP code sent to your email");
    }

    @PostMapping("/access-mode/change")
    public ResponseEntity<String> changeAccessMode(
            @RequestBody Map<String, String> request,
            Principal principal) {
        // Exception handling is done via GlobalExceptionHandler
        String newModeStr = request.get("mode");
        String password = request.get("password");
        String otpCode = request.get("otpCode");
        String reason = request.get("reason");

        if (newModeStr == null || password == null || otpCode == null) {
            return ResponseEntity.badRequest().body("Mode, password, and OTP code are required");
        }

        AccessMode newMode;
        try {
            newMode = AccessMode.valueOf(newModeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid access mode: " + newModeStr);
        }

        adminService.changeAccessMode(newMode, principal.getName(), password, otpCode, 
                reason != null ? reason : "");
        return ResponseEntity.ok("Access mode changed successfully");
    }

    @PostMapping(ApiConstants.VERIFY_ADMIN_URL)
    public ResponseEntity<String> verifyAdmin(@RequestBody Map<String, String> request, Principal principal) {
        String password = request.get(SecurityConstants.PASSWORD_KEY);
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(MessageConstants.PASSWORD_IS_REQUIRED);
        }
        
        // Exception handling is done via GlobalExceptionHandler
        boolean verified = adminService.verifyAdminPassword(principal.getName(), password);
        if (!verified) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(MessageConstants.INVALID_PASSWORD);
        }
        
        return ResponseEntity.ok(MessageConstants.PASSWORD_VERIFIED);
    }
}