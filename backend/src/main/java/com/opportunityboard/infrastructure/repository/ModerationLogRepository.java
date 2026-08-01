package com.opportunityboard.infrastructure.repository;

import com.opportunityboard.domain.entity.ModerationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ModerationLogRepository extends JpaRepository<ModerationLog, UUID> {
}
