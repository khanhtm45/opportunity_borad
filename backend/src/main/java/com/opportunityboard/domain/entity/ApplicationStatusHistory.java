package com.opportunityboard.domain.entity;

import com.opportunityboard.domain.enums.AppStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "application_status_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApplicationStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", nullable = false)
    private Application application;

    @Enumerated(EnumType.STRING)
    private AppStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    @Column(columnDefinition = "text")
    private String note;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
