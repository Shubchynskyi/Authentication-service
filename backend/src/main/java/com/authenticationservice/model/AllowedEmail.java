package com.authenticationservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "allowed_emails")
public class AllowedEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(length = 1000)
    private String reason;

    public AllowedEmail(String email) {
        this.email = email;
    }

    public AllowedEmail(String email, String reason) {
        this.email = email;
        this.reason = reason;
    }
}