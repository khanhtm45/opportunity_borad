package com.opportunityboard.infrastructure.repository;

import com.opportunityboard.domain.entity.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, UUID> {
}
