package com.authenticationservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * History log for access mode changes.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "access_mode_change_log")
public class AccessModeChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_mode", nullable = false)
    private AccessMode oldMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_mode", nullable = false)
    private AccessMode newMode;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(length = 1000)
    private String reason;
}

