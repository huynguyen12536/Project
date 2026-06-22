package com.learnhub.user.repository;

import com.learnhub.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findByEmailVerifiedFalse();

    @Query("SELECT u FROM User u WHERE u.id = ?1 AND u.emailVerified = true")
    Optional<User> findByIdAndEmailVerifiedTrue(UUID id);

    Long countByEmailVerifiedTrue();
}
