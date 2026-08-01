package com.opportunityboard.infrastructure.repository;

import com.opportunityboard.domain.entity.SavedOpportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.UUID;

public interface SavedOpportunityRepository extends JpaRepository<SavedOpportunity, UUID> {

    Page<SavedOpportunity> findByStudentUserId(UUID studentId, Pageable pageable);

    boolean existsByStudentUserIdAndOpportunityOppId(UUID studentId, UUID oppId);

    // Cron: bookmark sắp hết hạn
    @Query(value = """
           SELECT s.* FROM saved_opportunities s
           JOIN opportunities o ON o.opp_id = s.opp_id
           WHERE o.status = 'APPROVED' AND o.deadline BETWEEN :now AND :now + (s.notify_before_hours || ' hours')::interval
           """, nativeQuery = true)
    java.util.List<SavedOpportunity> findDueDeadlineAlerts(@Param("now") Instant now);
}
