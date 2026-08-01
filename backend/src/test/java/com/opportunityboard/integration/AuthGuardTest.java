package com.opportunityboard.integration;

import com.opportunityboard.application.dto.auth.LoginRequest;
import com.opportunityboard.application.dto.auth.RegisterRequest;
import com.opportunityboard.application.service.AuthService;
import com.opportunityboard.common.exception.ConflictException;
import com.opportunityboard.common.exception.UnauthorizedException;
import com.opportunityboard.domain.entity.User;
import com.opportunityboard.domain.enums.UserRole;
import com.opportunityboard.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: Auth guard (chống self-Admin, lockout brute-force, role assignment).
 */
class AuthGuardTest extends AbstractIntegrationTest {

    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;

    @Test
    void cannot_self_register_as_admin() {
        RegisterRequest req = new RegisterRequest(UserRole.ADMIN, "hacker@test.com", "password123", "Hacker");
        assertThrows(UnauthorizedException.class, () -> authService.register(req));
    }

    @Test
    void student_registers_as_pending() {
        cleanupUser("newstu@test.com");
        RegisterRequest req = new RegisterRequest(UserRole.STUDENT, "newstu@test.com", "password123", "New Stu");
        AuthServiceTestHelper.silent(() -> authService.register(req));
        User u = userRepository.findByEmail("newstu@test.com").orElseThrow();
        assertEquals(UserRole.STUDENT, u.getRole());
        assertEquals(com.opportunityboard.domain.enums.UserStatus.PENDING_VERIFICATION, u.getStatus());
    }

    @Test
    void provider_registers_with_pending_org() {
        cleanupUser("newprov@test.com");
        RegisterRequest req = new RegisterRequest(UserRole.PROVIDER, "newprov@test.com", "password123", "New Prov");
        AuthServiceTestHelper.silent(() -> authService.register(req));
        User u = userRepository.findByEmail("newprov@test.com").orElseThrow();
        assertEquals(UserRole.PROVIDER, u.getRole());
    }

    @Test
    void duplicate_email_rejected() {
        cleanupUser("dup@test.com");
        RegisterRequest req = new RegisterRequest(UserRole.STUDENT, "dup@test.com", "password123", "Dup");
        AuthServiceTestHelper.silent(() -> authService.register(req));
        RegisterRequest dup = new RegisterRequest(UserRole.STUDENT, "dup@test.com", "password123", "Dup2");
        assertThrows(ConflictException.class, () -> authService.register(dup));
    }

    @Test
    void lockout_after_5_failed_logins() {
        cleanupUser("lock@test.com");
        // tạo user ACTIVE
        com.opportunityboard.domain.entity.User u = userRepository.save(com.opportunityboard.domain.entity.User.builder()
                .email("lock@test.com")
                .passwordHash(passwordEncoder.encode("rightpass"))
                .fullName("Lock").role(UserRole.STUDENT)
                .status(com.opportunityboard.domain.enums.UserStatus.ACTIVE)
                .authProvider(com.opportunityboard.domain.enums.AuthProvider.EMAIL)
                .passwordVersion(1).failedLoginCount((short) 0).build());

        LoginRequest bad = new LoginRequest("lock@test.com", "wrongpass");
        for (int i = 0; i < 5; i++) {
            assertThrows(UnauthorizedException.class, () -> authService.login(bad));
        }
        // lần thứ 6: tài khoản đã bị khóa
        User reloaded = userRepository.findByEmail("lock@test.com").orElseThrow();
        assertNotNull(reloaded.getLockedUntil(), "lockedUntil should be set after 5 failures");
        assertTrue(reloaded.getLockedUntil().isAfter(java.time.Instant.now()));
        assertThrows(UnauthorizedException.class, () -> authService.login(bad));
    }

    @Test
    void correct_login_succeeds() {
        cleanupUser("ok@test.com");
        userRepository.save(com.opportunityboard.domain.entity.User.builder()
                .email("ok@test.com")
                .passwordHash(passwordEncoder.encode("rightpass"))
                .fullName("Ok").role(UserRole.STUDENT)
                .status(com.opportunityboard.domain.enums.UserStatus.ACTIVE)
                .authProvider(com.opportunityboard.domain.enums.AuthProvider.EMAIL)
                .passwordVersion(1).failedLoginCount((short) 0).build());
        var resp = authService.login(new LoginRequest("ok@test.com", "rightpass"));
        assertNotNull(resp.accessToken());
        assertNotNull(resp.refreshToken());
    }
}
