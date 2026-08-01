package com.opportunityboard.domain.entity;

import com.opportunityboard.domain.enums.AuthProvider;
import com.opportunityboard.domain.enums.UserRole;
import com.opportunityboard.domain.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID userId;

    @Column(nullable = false, unique = true)
    private String email;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.EMAIL;

    private Instant emailVerifiedAt;
    private Instant lastLoginAt;

    @Builder.Default
    private boolean mfaEnabled = false;

    @Builder.Default
    private short failedLoginCount = 0;

    private Instant lockedUntil;

    @Builder.Default
    private int passwordVersion = 1;

    @Builder.Default
    private Instant createdAt = Instant.now();
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
