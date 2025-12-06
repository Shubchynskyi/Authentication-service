package com.authenticationservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * History log for changes in access lists (whitelist/blacklist).
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "access_list_change_log")
public class AccessListChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "list_type", nullable = false)
    private AccessListType listType;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessListAction action;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(length = 1000)
    private String reason;

    public enum AccessListType {
        WHITELIST,
        BLACKLIST
    }

    public enum AccessListAction {
        ADD,
        REMOVE
    }
}

