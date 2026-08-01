package com.opportunityboard.application.service;

import com.opportunityboard.domain.entity.Notification;
import com.opportunityboard.infrastructure.repository.NotificationPreferenceRepository;
import com.opportunityboard.infrastructure.repository.NotificationRepository;
import com.opportunityboard.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final CurrentUser currentUser;

    public record NotifPage(List<Notification> items, long total) {}

    public NotifPage list(int page, int size) {
        UUID userId = currentUser.getId();
        Page<Notification> p = notificationRepository.findByUserUserId(userId, PageRequest.of(page, size));
        return new NotifPage(p.getContent(), p.getTotalElements());
    }

    @Transactional
    public void markRead(UUID id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new com.opportunityboard.common.exception.NotFoundException("Không tồn tại"));
        if (!n.getUser().getUserId().equals(currentUser.getId()))
            throw new com.opportunityboard.common.exception.ForbiddenException("Không có quyền");
        n.setRead(true);
        notificationRepository.save(n);
    }

    public List<?> getPreferences() {
        return preferenceRepository.findByUserUserId(currentUser.getId());
    }
}
