package com.authenticationservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Singleton entity for storing masked login settings.
 * Only one record should exist (id = 1).
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "masked_login_settings")
public class MaskedLoginSettings {

    @Id
    @Column(nullable = false)
    private Long id = 1L;

    @Column(nullable = false)
    private Boolean enabled = false;

    @Column(name = "template_id", nullable = false)
    private Integer templateId = 1;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    public MaskedLoginSettings(Boolean enabled, Integer templateId) {
        this.enabled = enabled;
        this.templateId = templateId;
    }
}

