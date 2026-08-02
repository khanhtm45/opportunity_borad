package com.opportunityboard.infrastructure.repository;

import com.opportunityboard.domain.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {
    Optional<StudentProfile> findByUserUserId(UUID userId);
}
