package com.opportunityboard.infrastructure.repository;

import com.opportunityboard.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAll();
    long countByRole(com.opportunityboard.domain.enums.UserRole role);
}
