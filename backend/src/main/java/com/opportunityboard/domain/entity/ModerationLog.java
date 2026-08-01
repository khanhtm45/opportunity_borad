package com.opportunityboard.domain.entity;

import com.opportunityboard.domain.enums.ModerationAction;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "moderation_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ModerationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opp_id", nullable = false)
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModerationAction action;

    @Column(columnDefinition = "text")
    private String reason;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
