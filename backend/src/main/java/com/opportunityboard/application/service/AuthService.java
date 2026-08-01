package com.opportunityboard.application.service;

import com.opportunityboard.application.dto.auth.*;
import com.opportunityboard.common.exception.ConflictException;
import com.opportunityboard.common.exception.UnauthorizedException;
import com.opportunityboard.domain.entity.Organization;
import com.opportunityboard.domain.entity.User;
import com.opportunityboard.domain.enums.*;
import com.opportunityboard.infrastructure.repository.OrganizationRepository;
import com.opportunityboard.infrastructure.repository.UserRepository;
import com.opportunityboard.security.CurrentUser;
import com.opportunityboard.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CurrentUser currentUser;

    /**
     * A1.2: Role KHÔNG tự chọn tự do.
     * - STUDENT: tạo luôn, pending verify email.
     * - PROVIDER: tạo user + org ở VERIFIED=PENDING; CHỈ đăng tin sau Admin duyệt.
     * - ADMIN: không cho self-register.
     */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (req.role() == UserRole.ADMIN) {
            throw new UnauthorizedException("Không thể tự đăng ký quyền Admin");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new ConflictException("Email đã tồn tại");
        }
        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .role(req.role())
                .status(UserStatus.PENDING_VERIFICATION)
                .authProvider(AuthProvider.EMAIL)
                .build();
        user = userRepository.save(user);

        if (req.role() == UserRole.PROVIDER) {
            // Tạo org ở trạng thái chờ duyệt (verify sau)
            Organization org = Organization.builder()
                    .ownerUser(user)
                    .orgName(req.fullName() + " Org")
                    .verifiedStatus(OrgVerified.PENDING)
                    .build();
            organizationRepository.save(org);
        }
        return issueTokens(user);
    }

    /** A1.2: Provider đăng ký org + contact (không tự thành provider nếu chưa có). */
    @Transactional
    public AuthResponse registerProvider(ProviderRegisterRequest req) {
        if (userRepository.existsByEmail(req.contactEmail())) {
            throw new ConflictException("Email đã tồn tại");
        }
        User user = User.builder()
                .email(req.contactEmail())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.contactFullName())
                .role(UserRole.PROVIDER)
                .status(UserStatus.PENDING_VERIFICATION)
                .authProvider(AuthProvider.EMAIL)
                .build();
        user = userRepository.save(user);
        Organization org = Organization.builder()
                .ownerUser(user)
                .orgName(req.orgName())
                .website(req.website())
                .description(req.description())
                .contactEmail(req.contactEmail())
                .verifiedStatus(OrgVerified.PENDING)
                .build();
        organizationRepository.save(org);
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = { org.springframework.security.core.AuthenticationException.class,
            com.opportunityboard.common.exception.UnauthorizedException.class })
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new UnauthorizedException("Sai email hoặc mật khẩu"));

        // Lockout: nếu locked_until > now -> từ chối
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new UnauthorizedException("Tài khoản đang bị khóa tạm thời");
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        } catch (Exception ex) {
            // tăng failed_login_count, lock sau 5 lần
            user.setFailedLoginCount((short) (user.getFailedLoginCount() + 1));
            if (user.getFailedLoginCount() >= 5) {
                user.setLockedUntil(Instant.now().plusSeconds(900)); // 15 phút
                user.setFailedLoginCount((short) 0);
            }
            userRepository.save(user);
            throw new UnauthorizedException("Sai email hoặc mật khẩu");
        }
        // reset đếm sai
        user.setFailedLoginCount((short) 0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest req) {
        if (!jwtTokenProvider.isRefresh(req.refreshToken())) {
            throw new UnauthorizedException("Refresh token không hợp lệ");
        }
        UUID userId = jwtTokenProvider.getUserId(req.refreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User không tồn tại"));
        if (jwtTokenProvider.getPasswordVersion(req.refreshToken()) != user.getPasswordVersion()) {
            throw new UnauthorizedException("Token đã bị thu hồi (đổi mật khẩu)");
        }
        return issueTokens(user);
    }

    @Transactional
    public void logout() {
        // thu hồi: tăng password_version -> mọi token cũ vô hiệu
        User user = currentUser.get();
        user.setPasswordVersion(user.getPasswordVersion() + 1);
        userRepository.save(user);
    }

    private AuthResponse issueTokens(User user) {
        String access = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getRole().name(), user.getPasswordVersion());
        String refresh = jwtTokenProvider.generateRefreshToken(user.getUserId(), user.getPasswordVersion());
        return new AuthResponse(access, refresh, user.getUserId(), user.getRole().name(), user.getFullName());
    }
}
