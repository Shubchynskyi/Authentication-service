package com.authenticationservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Singleton entity for storing current access control mode.
 * Only one record should exist (id = 1).
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "access_mode_settings")
public class AccessModeSettings {

    @Id
    @Column(nullable = false)
    private Long id = 1L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessMode mode = AccessMode.WHITELIST;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(length = 1000)
    private String reason;

    public AccessModeSettings(AccessMode mode) {
        this.mode = mode;
    }
}

