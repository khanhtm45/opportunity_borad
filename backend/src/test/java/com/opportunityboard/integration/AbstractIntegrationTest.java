package com.opportunityboard.integration;

import com.opportunityboard.domain.entity.User;
import com.opportunityboard.domain.enums.UserRole;
import com.opportunityboard.infrastructure.repository.OpportunityRepository;
import com.opportunityboard.infrastructure.repository.OrganizationRepository;
import com.opportunityboard.infrastructure.repository.UserRepository;
import com.opportunityboard.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

/**
 * Base integration test — Postgres qua Testcontainers (ephemeral).
 * <p>
 * Trước đây profile {@code test} nối Supabase thật và {@code TRUNCATE} mọi bảng
 * mỗi lần CI → mất seed demo trên production. Không còn dùng DB shared.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractIntegrationTest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("ob_test")
            .withUsername("test")
            .withPassword("test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

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
     * Reset data trong container test (an toàn — không phải Supabase).
     * Giữ categories / flyway_schema_history.
     */
    @BeforeEach
    void resetDatabase() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT tablename FROM pg_tables
                WHERE schemaname = 'public'
                  AND tablename NOT IN ('categories', 'flyway_schema_history', 'domains')
                """,
                String.class);
        if (!tables.isEmpty()) {
            jdbcTemplate.execute("TRUNCATE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE");
        }
    }

    /** Xoá user (nếu tồn tại) để test idempotent. */
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
