package com.opportunityboard.infrastructure.scheduler;

import com.opportunityboard.application.service.NotificationService;
import com.opportunityboard.domain.entity.Opportunity;
import com.opportunityboard.domain.entity.SavedOpportunity;
import com.opportunityboard.infrastructure.repository.OpportunityRepository;
import com.opportunityboard.infrastructure.repository.SavedOpportunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BoardScheduler {

    private final OpportunityRepository opportunityRepository;
    private final SavedOpportunityRepository savedRepo;
    private final NotificationService notificationService;

    /** Mỗi giờ: opp APPROVED quá hạn -> EXPIRED. */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireOverdue() {
        int n = opportunityRepository.expireOverdue(Instant.now());
        // log nếu cần
    }

    /** Mỗi giờ: bookmark sắp hết hạn -> DEADLINE_ALERT (F04.2, 24-48h). */
    @Scheduled(cron = "30 0 * * * *")
    @Transactional
    public void deadlineAlerts() {
        List<SavedOpportunity> due = savedRepo.findDueDeadlineAlerts(Instant.now());
        for (SavedOpportunity s : due) {
            notificationService.notifyDeadlineAlert(s);
        }
    }
}
