package com.authenticationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.authenticationservice.model.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByResetPasswordToken(String token);

    Page<User> findByEmailContainingOrNameContaining(String email, String name, Pageable pageable);

    boolean existsByEmail(String email);

    Page<User> findByEmailNot(String email, Pageable pageable);

    Page<User> findByEmailNotAndEmailContainingOrNameContaining(String currentUserEmail, String emailSearch, String nameSearch, Pageable pageable);
}