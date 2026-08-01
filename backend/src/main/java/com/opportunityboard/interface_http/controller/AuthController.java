package com.opportunityboard.interface_http.controller;

import com.opportunityboard.application.dto.auth.*;
import com.opportunityboard.application.service.AuthService;
import com.opportunityboard.common.response.PagedResponse;
import com.opportunityboard.security.CurrentUser;
import com.opportunityboard.domain.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        User u = currentUser.get();
        return ResponseEntity.ok(Map.of(
                "userId", u.getUserId(),
                "email", u.getEmail(),
                "fullName", u.getFullName(),
                "role", u.getRole().name(),
                "status", u.getStatus().name()));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/providers/register")
    public ResponseEntity<AuthResponse> registerProvider(@Valid @RequestBody ProviderRegisterRequest req) {
        return ResponseEntity.ok(authService.registerProvider(req));
    }
}
