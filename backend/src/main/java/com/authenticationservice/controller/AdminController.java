package com.authenticationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.authenticationservice.constants.ApiConstants;
import com.authenticationservice.constants.MessageConstants;
import com.authenticationservice.dto.AccessListUpdateResponse;
import com.authenticationservice.dto.AdminUpdateUserRequest;
import com.authenticationservice.dto.AllowedEmailDTO;
import com.authenticationservice.dto.BlockedEmailDTO;
import com.authenticationservice.dto.ChangeAccessModeRequest;
import com.authenticationservice.dto.PagedResponse;
import com.authenticationservice.dto.UserDTO;
import com.authenticationservice.dto.UpdateUserRolesRequest;
import com.authenticationservice.dto.VerifyAdminRequest;
import com.authenticationservice.model.AccessMode;
import com.authenticationservice.model.AccessModeSettings;
import com.authenticationservice.model.Role;
import com.authenticationservice.service.AdminService;
import com.authenticationservice.repository.RoleRepository;
import com.authenticationservice.util.LoggingSanitizer;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import java.security.Principal;
import org.springframework.http.HttpStatus;

@Slf4j(topic = "com.authenticationservice.admin")
@RestController
@RequestMapping(ApiConstants.ADMIN_BASE_URL)
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final RoleRepository roleRepository;

    private String maskEmail(String email) {
        return LoggingSanitizer.maskEmail(email);
    }

    @GetMapping(ApiConstants.ROLES_URL)
    public ResponseEntity<List<String>> getAllRoles() {
        return ResponseEntity.ok(
            roleRepository.findAll().stream()
                .map(Role::getName)
                .collect(Collectors.toList())
        );
    }

    @GetMapping(ApiConstants.USERS_URL)
    public ResponseEntity<PagedResponse<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        Page<UserDTO> usersPage = adminService.getAllUsers(PageRequest.of(page, size), search);
        PagedResponse<UserDTO> response = new PagedResponse<>(
                usersPage.getContent(),
                usersPage.getNumber(),
                usersPage.getSize(),
                usersPage.getTotalElements(),
                usersPage.getTotalPages()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping(ApiConstants.USERS_URL)
    public ResponseEntity<String> createUser(@Valid @RequestBody AdminUpdateUserRequest request) {
        log.debug("Admin create user request received for email: {}", maskEmail(request.getEmail()));
        // Note: createUser automatically adds to whitelist and removes from blacklist
        adminService.createUser(request);
        log.info("Admin created user successfully: {}", maskEmail(request.getEmail()));
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
    public ResponseEntity<String> updateUser(@PathVariable Long id, @Valid @RequestBody AdminUpdateUserRequest request) {
        log.debug("Admin update user request received for user ID: {}, email: {}", id, maskEmail(request.getEmail()));
        adminService.updateUser(id, request);
        log.info("Admin updated user successfully: ID={}, email={}", id, maskEmail(request.getEmail()));
        return ResponseEntity.ok(MessageConstants.USER_UPDATED);
    }

    @DeleteMapping(ApiConstants.USER_ID_URL)
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        log.debug("Admin delete user request received for user ID: {}", id);
        adminService.deleteUser(id);
        log.info("Admin deleted user successfully: ID={}", id);
        return ResponseEntity.ok(MessageConstants.USER_DELETED);
    }

    @PutMapping(ApiConstants.USERS_ID_ROLES_URL)
    public ResponseEntity<?> updateUserRoles(@PathVariable Long id, @RequestBody UpdateUserRolesRequest request) {
        log.debug("Admin update user roles request received for user ID: {}, roles: {}", id, request.getRoles());
        UserDTO updated = adminService.updateUserRoles(id, request.getRoles());
        log.info("Admin updated user roles successfully: ID={}, roles={}", id, request.getRoles());
        return ResponseEntity.ok(updated);
    }

    @GetMapping(ApiConstants.WHITELIST_URL)
    public ResponseEntity<List<AllowedEmailDTO>> getWhitelist() {
        return ResponseEntity.ok(adminService.getWhitelist());
    }

    @PostMapping(ApiConstants.WHITELIST_ADD_URL)
    public ResponseEntity<String> addToWhitelist(
            @RequestParam String email,
            @RequestParam(required = false) String reason) {
        log.debug("Admin add to whitelist request received for email: {}, reason: {}", maskEmail(email), reason);
        adminService.addToWhitelist(email, reason != null ? reason : "");
        log.info("Admin added email to whitelist: {}", maskEmail(email));
        return ResponseEntity.ok(MessageConstants.EMAIL_ADDED_TO_WHITELIST);
    }

    @DeleteMapping(ApiConstants.WHITELIST_REMOVE_URL)
    public ResponseEntity<String> removeFromWhitelist(
            @RequestParam String email,
            @RequestParam(required = false) String reason) {
        log.debug("Admin remove from whitelist request received for email: {}, reason: {}", maskEmail(email), reason);
        adminService.removeFromWhitelist(email, reason != null ? reason : "");
        log.info("Admin removed email from whitelist: {}", maskEmail(email));
        return ResponseEntity.ok(MessageConstants.EMAIL_REMOVED_FROM_WHITELIST);
    }

    @GetMapping("/blacklist")
    public ResponseEntity<List<BlockedEmailDTO>> getBlacklist() {
        return ResponseEntity.ok(adminService.getBlacklist());
    }

    @PostMapping("/blacklist/add")
    public ResponseEntity<AccessListUpdateResponse> addToBlacklist(
            @RequestParam String email,
            @RequestParam(required = false) String reason) {
        log.debug("Admin add to blacklist request received for email: {}, reason: {}", maskEmail(email), reason);
        AccessListUpdateResponse response = adminService.addToBlacklist(email, reason != null ? reason : "");
        log.info("Admin added email to blacklist: {}", maskEmail(email));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/blacklist/remove")
    public ResponseEntity<String> removeFromBlacklist(
            @RequestParam String email,
            @RequestParam(required = false) String reason) {
        log.debug("Admin remove from blacklist request received for email: {}, reason: {}", maskEmail(email), reason);
        adminService.removeFromBlacklist(email, reason != null ? reason : "");
        log.info("Admin removed email from blacklist: {}", maskEmail(email));
        return ResponseEntity.ok(MessageConstants.EMAIL_REMOVED_FROM_BLACKLIST);
    }

    @GetMapping("/access-mode")
    public ResponseEntity<AccessModeSettings> getAccessMode() {
        return ResponseEntity.ok(adminService.getAccessModeSettings());
    }

    @PostMapping("/access-mode/request-otp")
    public ResponseEntity<String> requestModeChangeOtp(Principal principal) {
        adminService.sendModeChangeOtp(principal.getName());
        return ResponseEntity.ok("OTP code sent to your email");
    }

    @PostMapping("/access-mode/change")
    public ResponseEntity<String> changeAccessMode(
            @Valid @RequestBody ChangeAccessModeRequest request,
            Principal principal) {
        log.debug("Admin change access mode request received for mode: {}", request.getMode());
        AccessMode newMode;
        try {
            newMode = AccessMode.valueOf(request.getMode().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(MessageConstants.ACCESS_MODE_INVALID);
        }
        adminService.changeAccessMode(newMode, principal.getName(), request.getPassword(), request.getOtpCode(),
                request.getReason() != null ? request.getReason() : "");
        log.info("Admin changed access mode successfully to: {}", newMode);
        return ResponseEntity.ok(MessageConstants.ACCESS_MODE_CHANGED);
    }

    @PostMapping(ApiConstants.VERIFY_ADMIN_URL)
    public ResponseEntity<String> verifyAdmin(@RequestBody VerifyAdminRequest request, Principal principal) {
        String password = request != null ? request.getPassword() : null;
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(MessageConstants.PASSWORD_IS_REQUIRED);
        }
        boolean verified = adminService.verifyAdminPassword(principal.getName(), password);
        if (!verified) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(MessageConstants.INVALID_PASSWORD);
        }
        
        return ResponseEntity.ok(MessageConstants.PASSWORD_VERIFIED);
    }
}