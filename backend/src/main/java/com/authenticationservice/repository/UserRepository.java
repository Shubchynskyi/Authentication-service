package com.authenticationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
            SELECT u FROM User u
            WHERE u.email <> :currentEmail
              AND (
                LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<User> searchByEmailOrName(@Param("currentEmail") String currentUserEmail,
                                   @Param("search") String search,
                                   Pageable pageable);
}
