package com.opportunityboard.application.service;

import com.opportunityboard.domain.entity.User;
import com.opportunityboard.domain.enums.OrgVerified;
import com.opportunityboard.domain.enums.UserRole;
import com.opportunityboard.infrastructure.repository.ApplicationRepository;
import com.opportunityboard.infrastructure.repository.OpportunityRepository;
import com.opportunityboard.infrastructure.repository.OrganizationRepository;
import com.opportunityboard.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final OpportunityRepository opportunityRepository;
    private final OrganizationRepository organizationRepository;
    private final ApplicationRepository applicationRepository;

    /** Danh sách user (phân trang). */
    public Map<String, Object> listUsers(int page, int size) {
        Page<User> p = userRepository.findAll(PageRequest.of(page, size));
        Map<String, Object> out = new HashMap<>();
        out.put("items", p.getContent());
        out.put("total", p.getTotalElements());
        return out;
    }

    /** Thống kê toàn hệ thống. */
    public Map<String, Object> analytics() {
        Map<String, Object> m = new HashMap<>();
        m.put("users", userRepository.count());
        m.put("providers", userRepository.countByRole(UserRole.PROVIDER));
        m.put("students", userRepository.countByRole(UserRole.STUDENT));
        m.put("opportunities", opportunityRepository.count());
        m.put("applications", applicationRepository.count());
        m.put("generated_at", Instant.now().toString());
        return m;
    }

    /** Duyệt org thành VERIFIED (F06: verify provider). */
    @Transactional
    public void verifyOrg(UUID userId) {
        organizationRepository.findByOwnerUserUserId(userId).forEach(org -> {
            org.setVerifiedStatus(com.opportunityboard.domain.enums.OrgVerified.VERIFIED);
            org.setVerifiedAt(Instant.now());
            organizationRepository.save(org);
        });
    }
}
