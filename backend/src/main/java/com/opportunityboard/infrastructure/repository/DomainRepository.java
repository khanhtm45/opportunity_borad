package com.opportunityboard.infrastructure.repository;

import com.opportunityboard.domain.entity.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DomainRepository extends JpaRepository<Domain, UUID> {
}
