package com.authenticationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.authenticationservice.model.AllowedEmail;

import java.util.Optional;

public interface AllowedEmailRepository extends JpaRepository<AllowedEmail, Long> {
    Optional<AllowedEmail> findByEmail(String email);
}