package com.authenticationservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Entity for storing blocked emails (blacklist).
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "blocked_emails")
public class BlockedEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(length = 1000)
    private String reason;

    public BlockedEmail(String email) {
        this.email = email;
    }

    public BlockedEmail(String email, String reason) {
        this.email = email;
        this.reason = reason;
    }
}

