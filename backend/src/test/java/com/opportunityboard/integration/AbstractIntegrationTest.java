package com.opportunityboard.integration;

import com.opportunityboard.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import com.opportunityboard.domain.entity.User;
import com.opportunityboard.domain.enums.UserRole;
import com.opportunityboard.infrastructure.repository.OpportunityRepository;
import com.opportunityboard.infrastructure.repository.OrganizationRepository;
import com.opportunityboard.infrastructure.repository.UserRepository;

import java.util.List;

/**
 * Base class cho integration test.
 * Chạy trên profile "test" (mặc định Supabase thật, xem application-test.yml).
 * Khi Docker sẵn sàng, dùng TestcontainersPostgres thay thế datasource.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected OrganizationRepository organizationRepository;
    @Autowired
    protected OpportunityRepository opportunityRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected CurrentUser currentUser;
    @Autowired
    protected TransactionTemplate transactionTemplate;

    /** Xoá user (nếu tồn tại) để test idempotent trên cùng DB. */
    protected void cleanupUser(String email) {
        transactionTemplate.executeWithoutResult(status -> {
            userRepository.findByEmail(email).ifPresent(u -> {
                // xoá opportunity + org sở hữu để tránh FK
                organizationRepository.findByOwnerUserUserId(u.getUserId()).forEach(org -> {
                    opportunityRepository.deleteByOrgOrgId(org.getOrgId());
                    organizationRepository.delete(org);
                });
                userRepository.delete(u);
            });
        });
    }

    /** Tạo user + set SecurityContext (giả lập JWT đã xác thực). */
    protected User loginAs(UserRole role, String email) {
        cleanupUser(email);
        User u = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .fullName("Test " + role)
                .role(role)
                .status(com.opportunityboard.domain.enums.UserStatus.ACTIVE)
                .authProvider(com.opportunityboard.domain.enums.AuthProvider.EMAIL)
                .passwordVersion(1)
                .failedLoginCount((short) 0)
                .build());
        authenticate(u);
        return u;
    }

    protected void authenticate(User u) {
        var auth = new UsernamePasswordAuthenticationToken(
                u.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
        SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    protected void asUser(User u) {
        authenticate(u);
    }

    protected void clearAuth() {
        SecurityContextHolder.clearContext();
    }
}
