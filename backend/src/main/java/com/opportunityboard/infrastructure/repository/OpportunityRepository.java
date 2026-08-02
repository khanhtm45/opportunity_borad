package com.opportunityboard.infrastructure.repository;

import com.opportunityboard.domain.entity.Opportunity;
import com.opportunityboard.domain.enums.OppStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {

    void deleteByOrgOrgId(UUID orgId);

    org.springframework.data.domain.Page<Opportunity> findByCreatedByUserId(java.util.UUID userId, org.springframework.data.domain.Pageable pg);

    org.springframework.data.domain.Page<Opportunity> findByStatus(com.opportunityboard.domain.enums.OppStatus status, org.springframework.data.domain.Pageable pg);

    long countByStatus(OppStatus status);

    // F01: board công khai = APPROVED & chưa hết hạn (display OPEN/OPEN_SOON)
    @Query("""
           SELECT o FROM Opportunity o
           WHERE o.status = com.opportunityboard.domain.enums.OppStatus.APPROVED
             AND o.deadline > :now
           """)
    Page<Opportunity> findPublicBoard(Pageable pageable, @Param("now") Instant now);

    // F01: featured slider
    @Query("""
           SELECT o FROM Opportunity o
           WHERE o.isFeatured = TRUE
             AND o.status = com.opportunityboard.domain.enums.OppStatus.APPROVED
             AND (o.featuredUntil IS NULL OR o.featuredUntil > :now)
             AND o.deadline > :now
           """)
    Page<Opportunity> findFeatured(Pageable pageable, @Param("now") Instant now);

    // F02: multi-filter (JPQL — Hibernate tự xử lý kiểu enum/uuid, tránh lỗi cast native query)
    @Query("""
           SELECT o FROM Opportunity o
           WHERE o.status = com.opportunityboard.domain.enums.OppStatus.APPROVED
             AND o.deadline > :now
             AND (o.category.categoryId = COALESCE(:categoryId, o.category.categoryId))
             AND (o.workType = COALESCE(:workType, o.workType))
             AND (o.location = COALESCE(:location, o.location))
             AND (LOWER(o.title) LIKE LOWER(CONCAT('%', COALESCE(:q, ''), '%'))
                  OR LOWER(o.description) LIKE LOWER(CONCAT('%', COALESCE(:q, ''), '%')))
           """)
    org.springframework.data.domain.Page<Opportunity> search(
            @Param("q") String q,
            @Param("categoryId") UUID categoryId,
            @Param("workType") com.opportunityboard.domain.enums.WorkType workType,
            @Param("location") com.opportunityboard.domain.enums.LocationType location,
            @Param("now") Instant now,
            Pageable pageable);

    Optional<Opportunity> findBySlug(String slug);

    Page<Opportunity> findByOrgOrgId(UUID orgId, Pageable pageable);

    // Cron: opp APPROVED quá hạn -> EXPIRED
    @Query("""
           UPDATE Opportunity o SET o.status = com.opportunityboard.domain.enums.OppStatus.EXPIRED
           WHERE o.status = com.opportunityboard.domain.enums.OppStatus.APPROVED
             AND o.deadline <= :now
           """)
    int expireOverdue(@Param("now") Instant now);
}
