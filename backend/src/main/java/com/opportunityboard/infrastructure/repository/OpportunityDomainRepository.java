package com.opportunityboard.infrastructure.repository;

import com.opportunityboard.domain.entity.OpportunityDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OpportunityDomainRepository extends JpaRepository<OpportunityDomain, com.opportunityboard.domain.entity.OpportunityDomainId> {
    void deleteByOppId(UUID oppId);
}
