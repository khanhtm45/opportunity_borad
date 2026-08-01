package com.opportunityboard.infrastructure.repository;

import com.opportunityboard.domain.entity.OrgMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OrgMemberRepository extends JpaRepository<OrgMember, UUID> {
    List<OrgMember> findByOrgOrgId(UUID orgId);
    boolean existsByOrgOrgIdAndUserUserId(UUID orgId, UUID userId);
}
