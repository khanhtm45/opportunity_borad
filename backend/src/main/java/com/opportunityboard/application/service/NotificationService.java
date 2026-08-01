package com.opportunityboard.application.service;

import com.opportunityboard.domain.entity.Application;
import com.opportunityboard.domain.entity.Opportunity;
import com.opportunityboard.domain.entity.SavedOpportunity;
import com.opportunityboard.domain.enums.AppStatus;

/**
 * Notification service (Mục 5). Ở MVP: lưu bản ghi Notification vào DB.
 * Triển khai thực tế (email/push send) qua @Async / queue ở P1/P2.
 */
public interface NotificationService {
    void notifyPendingReview(Opportunity o);
    void notifyOppApproved(Opportunity o);
    void notifyOppRejected(Opportunity o);
    void notifyNewOpp(Opportunity o);
    void notifyAppStatus(Application app, AppStatus to);
    void notifyDeadlineAlert(SavedOpportunity s);
}
