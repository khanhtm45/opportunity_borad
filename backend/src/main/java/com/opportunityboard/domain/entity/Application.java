package com.opportunityboard.domain.entity;

import com.opportunityboard.domain.enums.AppStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "applications",
       uniqueConstraints = @UniqueConstraint(columnNames = {"opp_id", "student_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID appId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opp_id", nullable = false)
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Builder.Default
    private boolean isExternal = false;

    private String cvFile;
    @Column(columnDefinition = "text")
    private String coverLetter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AppStatus status = AppStatus.SUBMITTED;

    @Column(columnDefinition = "text")
    private String providerNote;

    @Column(columnDefinition = "text")
    private String rejectionReason;

    /** Ghi chú AI sau khi quét CV theo tiêu chuẩn nhà đăng. */
    @Column(columnDefinition = "text")
    private String aiModerationNote;

    private Instant aiScannedAt;

    /** Tiêu chuẩn screening lần quét gần nhất. */
    @Column(columnDefinition = "text")
    private String screeningCriteria;

    @Builder.Default
    private Instant appliedAt = Instant.now();
    private Instant reviewedAt;
    private Instant interviewedAt;
    private Instant decidedAt;

    @Builder.Default
    private Instant updatedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;
}
