package com.opportunityboard.application.service;

import com.opportunityboard.application.dto.application.ApplicationSummaryResponse;
import com.opportunityboard.common.exception.NotFoundException;
import com.opportunityboard.domain.entity.Application;
import com.opportunityboard.domain.entity.Organization;
import com.opportunityboard.domain.entity.StudentProfile;
import com.opportunityboard.domain.entity.User;
import com.opportunityboard.domain.enums.AppStatus;
import com.opportunityboard.domain.enums.OppStatus;
import com.opportunityboard.domain.enums.OrgVerified;
import com.opportunityboard.domain.enums.UserRole;
import com.opportunityboard.infrastructure.repository.ApplicationRepository;
import com.opportunityboard.infrastructure.repository.OpportunityRepository;
import com.opportunityboard.infrastructure.repository.OrganizationRepository;
import com.opportunityboard.infrastructure.repository.StudentProfileRepository;
import com.opportunityboard.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final OpportunityRepository opportunityRepository;
    private final OrganizationRepository organizationRepository;
    private final ApplicationRepository applicationRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final OrgDocumentService orgDocumentService;

    public Map<String, Object> listUsers(int page, int size) {
        Page<User> p = userRepository.findAll(PageRequest.of(page, size));
        Map<String, Object> out = new HashMap<>();
        out.put("items", p.getContent());
        out.put("total", p.getTotalElements());
        return out;
    }

    public Map<String, Object> analytics() {
        Map<String, Object> m = new HashMap<>();
        long users = userRepository.count();
        long providers = userRepository.countByRole(UserRole.PROVIDER);
        long students = userRepository.countByRole(UserRole.STUDENT);
        long opportunities = opportunityRepository.count();
        long applications = applicationRepository.count();
        long pending = opportunityRepository.countByStatus(OppStatus.PENDING);
        long approved = opportunityRepository.countByStatus(OppStatus.APPROVED);
        long rejected = opportunityRepository.countByStatus(OppStatus.REJECTED);
        long draft = opportunityRepository.countByStatus(OppStatus.DRAFT);
        long orgs = organizationRepository.count();
        long orgsVerified = organizationRepository.countByVerifiedStatus(OrgVerified.VERIFIED);
        long orgsNeeds = organizationRepository.countByVerifiedStatus(OrgVerified.NEEDS_UPDATE);

        m.put("users", users);
        m.put("providers", providers);
        m.put("students", students);
        m.put("admins", Math.max(0, users - providers - students));
        m.put("opportunities", opportunities);
        m.put("applications", applications);
        m.put("pending", pending);
        m.put("approved", approved);
        m.put("rejected", rejected);
        m.put("draft", draft);
        m.put("orgs", orgs);
        m.put("orgsVerified", orgsVerified);
        m.put("orgsNeedsUpdate", orgsNeeds);

        List<Map<String, Object>> oppByStatus = new ArrayList<>();
        for (OppStatus s : List.of(OppStatus.PENDING, OppStatus.APPROVED, OppStatus.REJECTED,
                OppStatus.DRAFT, OppStatus.HIDDEN, OppStatus.CLOSED, OppStatus.EXPIRED)) {
            long c = opportunityRepository.countByStatus(s);
            if (c > 0) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("status", s.name());
                row.put("count", c);
                oppByStatus.add(row);
            }
        }
        m.put("oppByStatus", oppByStatus);

        List<Map<String, Object>> usersByRole = new ArrayList<>();
        usersByRole.add(Map.of("role", "STUDENT", "count", students));
        usersByRole.add(Map.of("role", "PROVIDER", "count", providers));
        usersByRole.add(Map.of("role", "ADMIN", "count", Math.max(0, users - providers - students)));
        m.put("usersByRole", usersByRole);

        m.put("generated_at", Instant.now().toString());
        return m;
    }

    /** Hàng đợi / danh sách ứng tuyển cho Admin (mặc định SUBMITTED+REVIEWING). */
    public Map<String, Object> listApplications(int page, int size, UUID oppId) {
        Page<Application> p;
        if (oppId != null) {
            p = applicationRepository.findByOpportunityOppId(oppId, PageRequest.of(page, size));
        } else {
            p = applicationRepository.findByStatusIn(
                    List.of(AppStatus.SUBMITTED, AppStatus.REVIEWING),
                    PageRequest.of(page, size));
        }
        List<ApplicationSummaryResponse> items = p.getContent().stream()
                .map(this::toAppSummary)
                .collect(Collectors.toList());
        Map<String, Object> out = new HashMap<>();
        out.put("items", items);
        out.put("total", p.getTotalElements());
        return out;
    }

    private ApplicationSummaryResponse toAppSummary(Application a) {
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

    @Transactional
    public void verifyOrg(UUID userId) {
        List<Organization> orgs = organizationRepository.findByOwnerUserUserId(userId);
        if (orgs.isEmpty()) {
            throw new NotFoundException("Không tìm thấy tổ chức của user");
        }
        for (Organization org : orgs) {
            orgDocumentService.requireHasDocuments(org.getOrgId());
            org.setVerifiedStatus(OrgVerified.VERIFIED);
            org.setVerifiedAt(Instant.now());
            organizationRepository.save(org);
        }
    }
}
