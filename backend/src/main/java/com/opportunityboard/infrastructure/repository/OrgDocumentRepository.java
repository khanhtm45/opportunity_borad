package com.opportunityboard.infrastructure.repository;

import com.opportunityboard.domain.entity.OrgDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrgDocumentRepository extends JpaRepository<OrgDocument, UUID> {
    List<OrgDocument> findByOrgOrgId(UUID orgId);
    long countByOrgOrgId(UUID orgId);
    void deleteByOrgOrgId(UUID orgId);
}
