package com.opportunityboard.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saved_opportunities",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "opp_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SavedOpportunity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opp_id", nullable = false)
    private Opportunity opportunity;

    @Builder.Default
    private Short notifyBeforeHours = 48; // [24,48]

    @Builder.Default
    private Instant savedAt = Instant.now();
}
