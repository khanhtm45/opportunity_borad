package com.opportunityboard.application.service;

import com.opportunityboard.application.dto.opportunity.OpportunityResponse;
import com.opportunityboard.common.exception.ConflictException;
import com.opportunityboard.common.exception.NotFoundException;
import com.opportunityboard.domain.entity.Opportunity;
import com.opportunityboard.domain.entity.SavedOpportunity;
import com.opportunityboard.domain.entity.User;
import com.opportunityboard.infrastructure.repository.OpportunityRepository;
import com.opportunityboard.infrastructure.repository.SavedOpportunityRepository;
import com.opportunityboard.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SavedOpportunityService {

    private final SavedOpportunityRepository savedRepo;
    private final OpportunityRepository opportunityRepository;
    private final CurrentUser currentUser;

    @Transactional
    public void save(UUID oppId, short notifyBeforeHours) {
        if (notifyBeforeHours < 24 || notifyBeforeHours > 48)
            throw new ConflictException("notifyBeforeHours phải trong [24,48]");
        User student = currentUser.get();
        Opportunity o = opportunityRepository.findById(oppId)
                .orElseThrow(() -> new NotFoundException("Opportunity không tồn tại"));
        if (savedRepo.existsByStudentUserIdAndOpportunityOppId(student.getUserId(), oppId)) return;
        savedRepo.save(SavedOpportunity.builder()
                .student(student).opportunity(o).notifyBeforeHours(notifyBeforeHours)
                .savedAt(Instant.now()).build());
        o.setBookmarkCount(o.getBookmarkCount() + 1);
        opportunityRepository.save(o);
    }

    @Transactional
    public void unsave(UUID oppId) {
        User student = currentUser.get();
        savedRepo.findByStudentUserId(student.getUserId(), PageRequest.of(0, 1)).getContent().stream()
                .filter(s -> s.getOpportunity().getOppId().equals(oppId))
                .findFirst().ifPresent(s -> {
                    savedRepo.delete(s);
                    Opportunity o = s.getOpportunity();
                    if (o.getBookmarkCount() > 0) { o.setBookmarkCount(o.getBookmarkCount() - 1); opportunityRepository.save(o); }
                });
    }

    public record SavedPage(List<OpportunityResponse> items, long total) {}

    public SavedPage list(int page, int size) {
        User student = currentUser.get();
        Page<SavedOpportunity> p = savedRepo.findByStudentUserId(student.getUserId(), PageRequest.of(page, size));
        List<OpportunityResponse> items = p.getContent().stream().map(s -> toResponse(s)).toList();
        return new SavedPage(items, p.getTotalElements());
    }

    private OpportunityResponse toResponse(SavedOpportunity s) {
        Opportunity o = s.getOpportunity();
        String logo = o.getLogoUrl() != null && !o.getLogoUrl().isBlank()
                ? o.getLogoUrl() : o.getOrg().getLogoUrl();
        return new OpportunityResponse(
                o.getOppId(), o.getTitle(), o.getSlug(),
                o.getOrg().getOrgName(), logo, o.getBannerUrl(),
                o.getCategory().getCode(), null,
                o.getDeadline(), o.getWorkType(),
                o.getLocation(), o.isFeatured(),
                o.getViewCount(), o.getBookmarkCount(),
                o.getApplicationCount(), o.getShareCount());
    }
}
