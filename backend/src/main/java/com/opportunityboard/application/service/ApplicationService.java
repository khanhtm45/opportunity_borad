package com.opportunityboard.application.service;

import com.opportunityboard.application.dto.application.ApplicationSummaryResponse;
import com.opportunityboard.common.exception.*;
import com.opportunityboard.domain.entity.*;
import com.opportunityboard.domain.enums.*;
import com.opportunityboard.infrastructure.repository.ApplicationRepository;
import com.opportunityboard.infrastructure.repository.ApplicationStatusHistoryRepository;
import com.opportunityboard.infrastructure.repository.OpportunityRepository;
import com.opportunityboard.infrastructure.repository.OrgMemberRepository;
import com.opportunityboard.infrastructure.repository.StudentProfileRepository;
import com.opportunityboard.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final OpportunityRepository opportunityRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final NotificationService notificationService;
    private final StudentProfileService studentProfileService;
    private final CurrentUser currentUser;

    /** F04.1: Sinh viên nộp CV nội bộ. Chặn EXTERNAL / trùng / quá hạn. */
    @Transactional
    public UUID applyInternal(UUID oppId, String cvFile, String coverLetter) {
        User student = currentUser.get();
        if (student.getRole() != UserRole.STUDENT) throw new ForbiddenException("Chỉ Student được ứng tuyển");
        Opportunity o = opportunityRepository.findById(oppId)
                .orElseThrow(() -> new NotFoundException("Opportunity không tồn tại"));
        if (o.getApplyMode() == ApplyMode.EXTERNAL)
            throw new ConflictException("Opportunity này dùng link ngoài, không nộp CV nội bộ");
        if (o.getStatus() != OppStatus.APPROVED)
            throw new ConflictException("Opportunity chưa mở"); // không OPEN
        if (o.getDeadline().isBefore(Instant.now()))
            throw new ConflictException("Đã quá hạn nộp");
        if (applicationRepository.existsByOpportunityOppIdAndStudentUserId(oppId, student.getUserId()))
            throw new ConflictException("Bạn đã nộp opportunity này");

        String cv = (cvFile != null && !cvFile.isBlank())
                ? cvFile.trim()
                : studentProfileService.resolveCvUrlForCurrentStudent();
        if (cv == null || cv.isBlank()) {
            throw new BadRequestException("Cần tải CV lên hồ sơ cá nhân trước khi nộp đơn (/me/profile)");
        }

        Application app = Application.builder()
                .opportunity(o).student(student).isExternal(false)
                .cvFile(cv).coverLetter(coverLetter)
                .status(AppStatus.SUBMITTED).appliedAt(Instant.now())
                .build();
        app = applicationRepository.save(app);

        // tăng counter
        o.setApplicationCount(o.getApplicationCount() + 1);
        opportunityRepository.save(o);
        return app.getAppId();
    }

    /** SV rút (chỉ SUBMITTED/REVIEWING). */
    @Transactional
    public void withdraw(UUID appId) {
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new NotFoundException("Không tồn tại"));
        User student = currentUser.get();
        if (!app.getStudent().getUserId().equals(student.getUserId()))
            throw new ForbiddenException("Không có quyền");
        if (app.getStatus() != AppStatus.SUBMITTED && app.getStatus() != AppStatus.REVIEWING)
            throw new ConflictException("Chỉ rút khi SUBMITTED/REVIEWING");
        changeStatus(app, AppStatus.WITHDRAWN, null, "Sinh viên rút");
        decrementCounter(app.getOpportunity());
    }

    /** F05.3: Provider đổi trạng thái (state machine enforcement). */
    @Transactional
    public void changeStatusByProvider(UUID appId, AppStatus to, String note) {
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new NotFoundException("Không tồn tại"));
        requireOwnerOfOpp(app.getOpportunity());
        changeStatus(app, to, note, null);
        notificationService.notifyAppStatus(app, to);
    }

    /** Lưu ghi chú AI scan — không đổi status. */
    @Transactional
    public void saveAiScanNote(UUID appId, String note, String criteria) {
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new NotFoundException("Không tồn tại"));
        requireOwnerOfOpp(app.getOpportunity());
        app.setAiModerationNote(note);
        app.setAiScannedAt(Instant.now());
        if (criteria != null && !criteria.isBlank()) {
            app.setScreeningCriteria(criteria.trim());
        }
        app.setUpdatedAt(Instant.now());
        applicationRepository.save(app);
    }

    /**
     * Gửi yêu cầu sinh viên cập nhật hồ sơ (giữ / chuyển REVIEWING, không REJECT).
     * Lý do hiện trên đơn SV + notification APP_UPDATE_REQUIRED.
     */
    @Transactional
    public void requestStudentUpdate(UUID appId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Cần nhập lý do gửi sinh viên");
        }
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new NotFoundException("Không tồn tại"));
        requireOwnerOfOpp(app.getOpportunity());
        if (app.getStatus() != AppStatus.SUBMITTED && app.getStatus() != AppStatus.REVIEWING) {
            throw new ConflictException("Chỉ yêu cầu cập nhật khi SUBMITTED/REVIEWING");
        }
        String r = reason.trim();
        if (app.getStatus() == AppStatus.SUBMITTED) {
            changeStatus(app, AppStatus.REVIEWING, r, "REQUEST_UPDATE: " + r);
        } else {
            app.setProviderNote(r);
            app.setUpdatedAt(Instant.now());
            app.setUpdatedBy(currentUser.get());
            applicationRepository.save(app);
            historyRepository.save(ApplicationStatusHistory.builder()
                    .application(app).fromStatus(AppStatus.REVIEWING).toStatus(AppStatus.REVIEWING)
                    .changedBy(currentUser.get())
                    .note("REQUEST_UPDATE: " + r).build());
        }
        app.setAiModerationNote(r);
        app.setProviderNote(r);
        applicationRepository.save(app);
        notificationService.notifyAppUpdateRequired(app, r);
    }

    /** State machine: chỉ tiến, không lùi (Mục 2). */
    private void changeStatus(Application app, AppStatus to, String note, String systemNote) {
        AppStatus from = app.getStatus();
        if (!isForward(from, to))
            throw new ConflictException("Không thể chuyển " + from + " -> " + to + " (chỉ tiến, không lùi)");
        app.setStatus(to);
        app.setUpdatedBy(currentUser.get());
        app.setUpdatedAt(Instant.now());
        if (to == AppStatus.REVIEWING) app.setReviewedAt(Instant.now());
        if (to == AppStatus.INTERVIEW) app.setInterviewedAt(Instant.now());
        if (to == AppStatus.ACCEPTED || to == AppStatus.REJECTED) app.setDecidedAt(Instant.now());
        if (note != null && (to == AppStatus.REJECTED)) app.setRejectionReason(note);
        if (note != null) app.setProviderNote(note);
        applicationRepository.save(app);

        historyRepository.save(ApplicationStatusHistory.builder()
                .application(app).fromStatus(from).toStatus(to)
                .changedBy(currentUser.get())
                .note(systemNote != null ? systemNote : note).build());
    }

    /** Chặn IDOR: chỉ owner opp (org owner / member) hoặc admin. */
    public void requireOwnerOfOpp(Opportunity o) {
        User user = currentUser.get();
        if (user.getRole() == UserRole.ADMIN) return;
        if (user.getRole() != UserRole.PROVIDER) throw new ForbiddenException("Chỉ Provider/Admin");
        boolean owner = o.getOrg().getOwnerUser().getUserId().equals(user.getUserId())
                || orgMemberRepository.existsByOrgOrgIdAndUserUserId(o.getOrg().getOrgId(), user.getUserId());
        if (!owner) throw new ForbiddenException("Không có quyền với opportunity này");
    }

    public Opportunity requireOwnerOfOppId(UUID oppId) {
        Opportunity o = opportunityRepository.findById(oppId)
                .orElseThrow(() -> new NotFoundException("Opportunity không tồn tại"));
        requireOwnerOfOpp(o);
        return o;
    }

    public UUID currentUserId() {
        return currentUser.getId();
    }

    private void decrementCounter(Opportunity o) {
        if (o.getApplicationCount() > 0) {
            o.setApplicationCount(o.getApplicationCount() - 1);
            opportunityRepository.save(o);
        }
    }

    /** Forward transitions. */
    private boolean isForward(AppStatus from, AppStatus to) {
        if (from == to) return false;
        return switch (from) {
            case SUBMITTED -> to == AppStatus.REVIEWING || to == AppStatus.REJECTED || to == AppStatus.WITHDRAWN;
            case REVIEWING -> to == AppStatus.INTERVIEW || to == AppStatus.REJECTED || to == AppStatus.WITHDRAWN;
            case INTERVIEW -> to == AppStatus.ACCEPTED || to == AppStatus.REJECTED;
            default -> false; // ACCEPTED/REJECTED/WITHDRAWN terminal
        };
    }

    public record AppPage(java.util.List<ApplicationSummaryResponse> items, long total) {}

    public AppPage appsByStudent(UUID studentId, int page, int size) {
        Page<Application> p = applicationRepository.findByStudentUserId(studentId,
                PageRequest.of(page, size));
        var items = p.getContent().stream().map(this::toSummary).collect(Collectors.toList());
        return new AppPage(items, p.getTotalElements());
    }

    public AppPage appsByOppOwner(UUID oppId, int page, int size) {
        UUID userId = currentUser.getId();
        Page<Application> p = applicationRepository.findByOpportunityOwner(oppId, userId, PageRequest.of(page, size));
        var items = p.getContent().stream().map(this::toSummary).collect(Collectors.toList());
        return new AppPage(items, p.getTotalElements());
    }

    private ApplicationSummaryResponse toSummary(Application a) {
        StudentProfile profile = studentProfileRepository
                .findByUserUserId(a.getStudent().getUserId()).orElse(null);
        return ApplicationSummaryResponse.builder()
                .appId(a.getAppId())
                .oppId(a.getOpportunity().getOppId())
                .title(a.getOpportunity().getTitle())
                .slug(a.getOpportunity().getSlug())
                .orgName(a.getOpportunity().getOrg().getOrgName())
                .status(a.getStatus())
                .isExternal(a.isExternal())
                .cvFile(a.getCvFile())
                .appliedAt(a.getAppliedAt())
                .decidedAt(a.getDecidedAt())
                .studentName(a.getStudent().getFullName())
                .studentEmail(a.getStudent().getEmail())
                .coverLetter(a.getCoverLetter())
                .providerNote(a.getProviderNote())
                .rejectionReason(a.getRejectionReason())
                .aiModerationNote(a.getAiModerationNote())
                .aiScannedAt(a.getAiScannedAt())
                .screeningCriteria(a.getScreeningCriteria())
                .major(profile != null ? profile.getMajor() : null)
                .university(profile != null ? profile.getUniversity() : null)
                .universityYear(profile != null ? profile.getUniversityYear() : null)
                .skills(profile != null ? profile.getSkills() : null)
                .build();
    }
}
