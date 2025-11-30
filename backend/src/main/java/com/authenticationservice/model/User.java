package com.authenticationservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @Column(name = "reset_password_token_expiry")
    private LocalDateTime resetPasswordTokenExpiry;

    @Column(name = "is_enabled")
    private boolean enabled = true;

    @Column(name = "is_blocked")
    private boolean blocked = false;

    @Column(name = "block_reason")
    private String blockReason;

    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts = 0;

    @Column(name = "last_login_attempt")
    private LocalDateTime lastLoginAttempt;

    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @Column(name = "unblocked_at")
    private LocalDateTime unblockedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider")
    private AuthProvider authProvider = AuthProvider.LOCAL;

    public User() {
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
        this.lastLoginAttempt = LocalDateTime.now();
        if (this.failedLoginAttempts >= 10) {
            this.blocked = true;
            this.blockReason = "Maximum login attempts exceeded";
        }
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lastLoginAt = LocalDateTime.now();
    }

    //todo check usage
    public boolean isAccountNonLocked() {
        return !blocked;
    }
}