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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
     * Flyway đang tắt trên Supabase — đảm bảo cột/bảng mới có trước khi test chạy.
     * DDL idempotent (khớp V4/V5 migrations).
     */
    @BeforeAll
    void ensureSchema() {
        jdbcTemplate.execute("ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS external_ref VARCHAR(120)");
        jdbcTemplate.execute("""
                DO $$ BEGIN
                    CREATE TYPE org_doc_type AS ENUM ('BUSINESS_LICENSE', 'TAX_CODE', 'IDENTITY', 'OTHER');
                EXCEPTION WHEN duplicate_object THEN NULL;
                END $$
                """);
        jdbcTemplate.execute("""
                DO $$ BEGIN
                    CREATE TYPE opp_doc_type AS ENUM ('PROGRAM_PROOF', 'PARTNERSHIP_LETTER', 'OTHER');
                EXCEPTION WHEN duplicate_object THEN NULL;
                END $$
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS org_documents (
                    doc_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    org_id     UUID NOT NULL REFERENCES organizations(org_id) ON DELETE CASCADE,
                    doc_type   org_doc_type NOT NULL,
                    title      VARCHAR(200) NOT NULL,
                    file_url   VARCHAR(512) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_org_documents_org ON org_documents(org_id)");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS opportunity_documents (
                    doc_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    opp_id     UUID NOT NULL REFERENCES opportunities(opp_id) ON DELETE CASCADE,
                    doc_type   opp_doc_type NOT NULL,
                    title      VARCHAR(200) NOT NULL,
                    file_url   VARCHAR(512) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_opp_documents_opp ON opportunity_documents(opp_id)");
        // V6 — thuộc tính kiểu TopCV
        for (String typeSql : List.of(
                "CREATE TYPE job_level AS ENUM ('INTERN', 'STAFF', 'TEAM_LEAD', 'MANAGER', 'DIRECTOR', 'OTHER')",
                "CREATE TYPE experience_level AS ENUM ('NONE', 'UNDER_ONE_YEAR', 'ONE_TO_TWO', 'TWO_TO_THREE', 'THREE_TO_FIVE', 'FIVE_PLUS')",
                "CREATE TYPE education_level AS ENUM ('NONE', 'HIGH_SCHOOL', 'INTERMEDIATE', 'COLLEGE', 'UNIVERSITY', 'POSTGRAD')",
                "CREATE TYPE employment_type AS ENUM ('FULL_TIME', 'PART_TIME', 'CONTRACT', 'FREELANCE', 'OTHER')",
                "CREATE TYPE company_size AS ENUM ('SIZE_1_10', 'SIZE_11_50', 'SIZE_51_200', 'SIZE_201_500', 'SIZE_500_PLUS', 'UNKNOWN')"
        )) {
            jdbcTemplate.execute("DO $$ BEGIN " + typeSql + "; EXCEPTION WHEN duplicate_object THEN NULL; END $$");
        }
        jdbcTemplate.execute("""
                ALTER TABLE opportunities
                    ADD COLUMN IF NOT EXISTS job_level job_level,
                    ADD COLUMN IF NOT EXISTS experience_level experience_level,
                    ADD COLUMN IF NOT EXISTS education_level education_level,
                    ADD COLUMN IF NOT EXISTS headcount INT,
                    ADD COLUMN IF NOT EXISTS employment_type employment_type,
                    ADD COLUMN IF NOT EXISTS salary_min BIGINT,
                    ADD COLUMN IF NOT EXISTS salary_max BIGINT,
                    ADD COLUMN IF NOT EXISTS salary_currency VARCHAR(10) DEFAULT 'VND',
                    ADD COLUMN IF NOT EXISTS salary_negotiable BOOLEAN NOT NULL DEFAULT FALSE,
                    ADD COLUMN IF NOT EXISTS address_detail VARCHAR(500),
                    ADD COLUMN IF NOT EXISTS working_schedule TEXT,
                    ADD COLUMN IF NOT EXISTS skills TEXT
                """);
        jdbcTemplate.execute("""
                ALTER TABLE organizations
                    ADD COLUMN IF NOT EXISTS tax_code VARCHAR(40),
                    ADD COLUMN IF NOT EXISTS address VARCHAR(500),
                    ADD COLUMN IF NOT EXISTS industry VARCHAR(200),
                    ADD COLUMN IF NOT EXISTS company_size company_size
                """);
        // V7 — yêu cầu provider cập nhật khi AI phát hiện sai/thiếu
        jdbcTemplate.execute("""
                DO $$ BEGIN
                    ALTER TYPE org_verified ADD VALUE IF NOT EXISTS 'NEEDS_UPDATE';
                EXCEPTION WHEN duplicate_object THEN NULL;
                END $$
                """);
        jdbcTemplate.execute("""
                DO $$ BEGIN
                    ALTER TYPE notif_type ADD VALUE IF NOT EXISTS 'ORG_UPDATE_REQUIRED';
                EXCEPTION WHEN duplicate_object THEN NULL;
                END $$
                """);
        jdbcTemplate.execute("""
                ALTER TABLE organizations
                    ADD COLUMN IF NOT EXISTS verification_note TEXT,
                    ADD COLUMN IF NOT EXISTS ai_scanned_at TIMESTAMP
                """);
        // V8 — ghi chú AI moderation tin đăng
        jdbcTemplate.execute("""
                DO $$ BEGIN
                    ALTER TYPE notif_type ADD VALUE IF NOT EXISTS 'OPP_UPDATE_REQUIRED';
                EXCEPTION WHEN duplicate_object THEN NULL;
                END $$
                """);
        jdbcTemplate.execute("""
                ALTER TABLE opportunities
                    ADD COLUMN IF NOT EXISTS ai_moderation_note TEXT,
                    ADD COLUMN IF NOT EXISTS ai_scanned_at TIMESTAMP
                """);
    }

    /**
     * Reset toàn bộ dữ liệu test trước mỗi test method.
     * Test profile chạy trên Supabase THẬT (không có Testcontainers trên CI),
     * nên các test class share cùng DB — phải xoá sạch để test độc lập,
     * tránh vi phạm FK (applications/apportunities tham chiếu user).
     * Giữ nguyên bảng categories (đã seed sẵn 7 category) và flyway_schema_history.
     */
    @BeforeEach
    void resetDatabase() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT tablename FROM pg_tables
                WHERE schemaname = 'public'
                  AND tablename NOT IN ('categories', 'flyway_schema_history')
                """,
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
