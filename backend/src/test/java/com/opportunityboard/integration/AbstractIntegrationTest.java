package com.opportunityboard.integration;

import com.opportunityboard.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.BeforeEach;

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
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /**
     * Reset toàn bộ dữ liệu test trước mỗi test method.
     * Test profile chạy trên Supabase THẬT (không có Testcontainers trên CI),
     * nên các test class share cùng DB — phải xoá sạch để test độc lập,
     * tránh vi phạm FK (applications/apportunities tham chiếu user).
     * Giữ nguyên bảng categories (đã seed sẵn 7 category).
     */
    @BeforeEach
    void resetDatabase() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename <> 'categories'",
                String.class);
        if (!tables.isEmpty()) {
            jdbcTemplate.execute("TRUNCATE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE");
        }
    }

    /** Xoá user (nếu tồn tại) để test idempotent trên cùng DB. */
    protected void cleanupUser(String email) {
        transactionTemplate.executeWithoutResult(status -> {
            userRepository.findByEmail(email).ifPresent(userRepository::delete);
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
