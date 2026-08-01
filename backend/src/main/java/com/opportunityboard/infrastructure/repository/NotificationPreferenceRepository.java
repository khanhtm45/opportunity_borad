package com.opportunityboard.infrastructure.repository;

import com.opportunityboard.domain.entity.NotificationPreference;
import com.opportunityboard.domain.enums.NotifChannel;
import com.opportunityboard.domain.enums.NotifType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, com.opportunityboard.domain.entity.NotificationPreferenceId> {
    List<NotificationPreference> findByUserUserId(UUID userId);

    // NEW_OPP matching: user bật category/domain khớp opp
    List<NotificationPreference> findByTypeAndChannelAndEnabledTrue(NotifType type, NotifChannel channel);
}
