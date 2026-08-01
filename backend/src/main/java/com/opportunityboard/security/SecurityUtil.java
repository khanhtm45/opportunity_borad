package com.opportunityboard.security;

import com.opportunityboard.domain.entity.User;
import com.opportunityboard.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Lấy userId / User hiện tại từ SecurityContext (principal = email).
 * Là Spring component (không dùng static holder để tránh circular dependency).
 */
@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UserRepository userRepository;

    public UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            throw new com.opportunityboard.common.exception.UnauthorizedException("Chưa xác thực");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new com.opportunityboard.common.exception.UnauthorizedException("User không tồn tại"))
                .getUserId();
    }

    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            throw new com.opportunityboard.common.exception.UnauthorizedException("Chưa xác thực");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new com.opportunityboard.common.exception.UnauthorizedException("User không tồn tại"));
    }
}
