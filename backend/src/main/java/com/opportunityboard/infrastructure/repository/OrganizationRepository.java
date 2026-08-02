package com.opportunityboard.infrastructure.repository;

import com.opportunityboard.domain.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    List<Organization> findByOwnerUserUserId(UUID ownerUserId);
    List<Organization> findByVerifiedStatus(com.opportunityboard.domain.enums.OrgVerified status);
    long countByVerifiedStatus(com.opportunityboard.domain.enums.OrgVerified status);
}
