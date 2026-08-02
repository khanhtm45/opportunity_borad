package com.opportunityboard.application.service;

import com.opportunityboard.domain.entity.Application;
import com.opportunityboard.domain.entity.Opportunity;
import com.opportunityboard.domain.entity.Organization;
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
    void notifyOppUpdateRequired(Opportunity o, String reason);
    void notifyNewOpp(Opportunity o);
    void notifyAppStatus(Application app, AppStatus to);
    void notifyDeadlineAlert(SavedOpportunity s);
    /** Yêu cầu provider cập nhật / bổ sung hồ sơ tổ chức (AI scan sai/thiếu). */
    void notifyOrgUpdateRequired(Organization org, String reason);

    void notifyOppUpdateRequired(Opportunity o, String reason);
}
