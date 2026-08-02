package com.opportunityboard.application.service;

import com.opportunityboard.domain.entity.*;
import com.opportunityboard.domain.enums.*;
import com.opportunityboard.infrastructure.repository.NotificationPreferenceRepository;
import com.opportunityboard.infrastructure.repository.NotificationRepository;
import com.opportunityboard.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    private void save(User user, NotifType type, NotifChannel channel, String title, String body, UUID refId) {
        notificationRepository.save(Notification.builder()
                .user(user).type(type).channel(channel).title(title).body(body)
                .refId(refId).createdAt(Instant.now()).build());
    }

    @Override
    public void notifyPendingReview(Opportunity o) {
        // gửi cho tất cả Admin (PENDING_REVIEW)
        userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ADMIN)
                .forEach(admin -> save(admin, NotifType.PENDING_REVIEW, NotifChannel.EMAIL,
                        "Tin chờ duyệt", "Opportunity '" + o.getTitle() + "' chờ duyệt", o.getOppId()));
    }

    @Override
    public void notifyOppApproved(Opportunity o) {
        save(o.getOrg().getOwnerUser(), NotifType.OPP_APPROVED, NotifChannel.EMAIL,
                "Tin đã duyệt", "'" + o.getTitle() + "' đã được duyệt", o.getOppId());
    }

    @Override
    public void notifyOppRejected(Opportunity o) {
        save(o.getOrg().getOwnerUser(), NotifType.OPP_REJECTED, NotifChannel.EMAIL,
                "Tin bị từ chối", "'" + o.getTitle() + "': " + o.getRejectionReason(), o.getOppId());
    }

    @Override
    @Transactional
    public void notifyNewOpp(Opportunity o) {
        // Quét preference: user bật NEW_OPP + khớp category/domain
        List<NotificationPreference> prefs = preferenceRepository
                .findByTypeAndChannelAndEnabledTrue(NotifType.NEW_OPP, NotifChannel.EMAIL);
        for (NotificationPreference p : prefs) {
            boolean matchCat = p.getCategories() != null && p.getCategories().contains(o.getCategory().getCategoryId().toString());
            if (matchCat) {
                save(p.getUser(), NotifType.NEW_OPP, NotifChannel.EMAIL,
                        "Cơ hội mới", "'" + o.getTitle() + "' vừa mở", o.getOppId());
            }
        }
    }

    @Override
    public void notifyDeadlineAlert(SavedOpportunity s) {
        Opportunity o = s.getOpportunity();
        save(s.getStudent(), NotifType.DEADLINE_ALERT, NotifChannel.EMAIL,
                "Sắp hết hạn", "Cơ hội '" + o.getTitle() + "' hết hạn trong " + s.getNotifyBeforeHours() + "h",
                o.getOppId());
    }

    @Override
    public void notifyAppStatus(Application app, AppStatus to) {
        String title = switch (to) {
            case REVIEWING -> "Hồ sơ đang được xem xét";
            case INTERVIEW -> "Bạn được mời phỏng vấn";
            case ACCEPTED -> "Chúc mừng! Trúng tuyển";
            case REJECTED -> "Kết quả ứng tuyển";
            case WITHDRAWN -> "Đã rút hồ sơ";
            default -> "Cập nhật hồ sơ";
        };
        save(app.getStudent(), NotifType.APP_STATUS, NotifChannel.EMAIL, title,
                "Opportunity '" + app.getOpportunity().getTitle() + "'", app.getAppId());
    }

    @Override
    public void notifyOrgUpdateRequired(Organization org, String reason) {
        User owner = org.getOwnerUser();
        if (owner == null) return;
        String body = "Hồ sơ tổ chức \"" + org.getOrgName() + "\" cần cập nhật lại. "
                + "Vui lòng bổ sung giấy tờ / sửa thông tin liên hệ rồi gửi lại để kiểm duyệt. "
                + (reason != null && !reason.isBlank() ? "Chi tiết: " + reason : "");
        save(owner, NotifType.ORG_UPDATE_REQUIRED, NotifChannel.IN_APP,
                "Yêu cầu cập nhật hồ sơ tổ chức", body, org.getOrgId());
        save(owner, NotifType.ORG_UPDATE_REQUIRED, NotifChannel.EMAIL,
                "Yêu cầu cập nhật hồ sơ tổ chức", body, org.getOrgId());
    }

    @Override
    public void notifyOppUpdateRequired(Opportunity o, String reason) {
        User owner = o.getCreatedBy();
        if (owner == null && o.getOrg() != null) owner = o.getOrg().getOwnerUser();
        if (owner == null) return;
        String body = "Tin \"" + o.getTitle() + "\" cần bổ sung hồ sơ / cập nhật lại trước khi duyệt. "
                + (reason != null && !reason.isBlank() ? "Lý do: " + reason : "");
        save(owner, NotifType.OPP_UPDATE_REQUIRED, NotifChannel.IN_APP,
                "Yêu cầu cập nhật tin đăng", body, o.getOppId());
        save(owner, NotifType.OPP_UPDATE_REQUIRED, NotifChannel.EMAIL,
                "Yêu cầu cập nhật tin đăng", body, o.getOppId());
    }

    @Override
    public void notifyAppUpdateRequired(Application app, String reason) {
        String body = "Đơn ứng tuyển \"" + app.getOpportunity().getTitle() + "\" cần bổ sung / cập nhật hồ sơ. "
                + "Vui lòng cập nhật CV trên hồ sơ cá nhân rồi liên hệ nhà tuyển dụng hoặc nộp lại nếu cần. "
                + (reason != null && !reason.isBlank() ? "Lý do: " + reason : "");
        save(app.getStudent(), NotifType.APP_UPDATE_REQUIRED, NotifChannel.IN_APP,
                "Yêu cầu cập nhật hồ sơ ứng tuyển", body, app.getAppId());
        save(app.getStudent(), NotifType.APP_UPDATE_REQUIRED, NotifChannel.EMAIL,
                "Yêu cầu cập nhật hồ sơ ứng tuyển", body, app.getAppId());
    }
}
