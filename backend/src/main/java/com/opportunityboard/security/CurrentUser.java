package com.opportunityboard.security;

import com.opportunityboard.domain.entity.User;
import com.opportunityboard.domain.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Helper lấy User hiện tại từ SecurityContext.
 * principal trong JWTAuthFilter là email (UserDetails.username).
 * Dùng email để load entity qua UserRepository.
 */
@Component
@RequiredArgsConstructor
public class CurrentUser {

    private final com.opportunityboard.infrastructure.repository.UserRepository userRepository;

    public User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new com.opportunityboard.common.exception.UnauthorizedException("Chưa xác thực");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new com.opportunityboard.common.exception.UnauthorizedException("User không tồn tại"));
    }

    public UUID getId() {
        return get().getUserId();
    }

    public UserRole getRole() {
        return get().getRole();
    }
}
