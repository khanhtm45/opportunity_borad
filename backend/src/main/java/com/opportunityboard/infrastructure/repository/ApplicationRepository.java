package com.opportunityboard.infrastructure.repository;

import com.opportunityboard.domain.entity.Application;
import com.opportunityboard.domain.enums.AppStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    boolean existsByOpportunityOppIdAndStudentUserId(UUID oppId, UUID studentId);

    Page<Application> findByStudentUserId(UUID studentId, Pageable pageable);

    Page<Application> findByStatusIn(java.util.Collection<AppStatus> statuses, Pageable pageable);

    Page<Application> findByOpportunityOppId(UUID oppId, Pageable pageable);

    @Query("""
           SELECT a FROM Application a
           JOIN a.opportunity o JOIN o.org org
           WHERE o.oppId = :oppId AND (
             org.ownerUser.userId = :userId
             OR EXISTS (SELECT 1 FROM OrgMember m WHERE m.org = org AND m.user.userId = :userId)
           )
           """)
    Page<Application> findByOpportunityOwner(
            @Param("oppId") UUID oppId,
            @Param("userId") UUID userId,
            Pageable pageable);

    @Query("""
           SELECT a FROM Application a
           JOIN a.opportunity o JOIN o.org org
           WHERE org.ownerUser.userId = :userId
              OR EXISTS (SELECT 1 FROM OrgMember m WHERE m.org = org AND m.user.userId = :userId)
           """)
    Page<Application> findByOrgOwner(@Param("userId") UUID userId, Pageable pageable);
}
