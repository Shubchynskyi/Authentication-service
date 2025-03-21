package com.authenticationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.authenticationservice.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByResetPasswordToken(String token);
}