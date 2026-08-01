package com.opportunityboard.infrastructure.repository;

import com.opportunityboard.domain.entity.Notification;
import com.opportunityboard.domain.enums.NotifType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByUserUserIdAndIsReadFalse(UUID userId, Pageable pageable);
    Page<Notification> findByUserUserId(UUID userId, Pageable pageable);
}
