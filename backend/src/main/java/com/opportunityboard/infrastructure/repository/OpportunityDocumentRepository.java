package com.opportunityboard.infrastructure.repository;

import com.opportunityboard.domain.entity.OpportunityDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface OpportunityDocumentRepository extends JpaRepository<OpportunityDocument, UUID> {
    List<OpportunityDocument> findByOpportunityOppId(UUID oppId);
    long countByOpportunityOppId(UUID oppId);

    @Modifying(clearAutomatically = true)
    @Transactional
    void deleteByOpportunityOppId(UUID oppId);
}
