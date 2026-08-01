package com.opportunityboard.domain.entity;

import com.opportunityboard.domain.enums.ApplyMode;
import com.opportunityboard.domain.enums.LocationType;
import com.opportunityboard.domain.enums.OppStatus;
import com.opportunityboard.domain.enums.WorkType;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "opportunities",
       uniqueConstraints = @UniqueConstraint(columnNames = "slug"),
       indexes = {
           @Index(name = "idx_opps_status_deadline", columnList = "status, deadline"),
           @Index(name = "idx_opps_category", columnList = "category_id"),
           @Index(name = "idx_opps_featured", columnList = "is_featured, featured_until")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Opportunity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID oppId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization org;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String title;

    @Column(name = "logo_url")
    private String logoUrl; // logo riêng của cơ hội (ưu tiên); fallback org.logoUrl

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false, columnDefinition = "text") // sanitized HTML / markdown
    private String description;

    @Column(columnDefinition = "text")
    private String requirements;

    @Column(columnDefinition = "text")
    private String benefits;

    @Column(name = "salary_or_reward", columnDefinition = "text")
    private String salaryOrReward;

    @Column(name = "selection_process", columnDefinition = "text")
    private String selectionProcess;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LocationType location = LocationType.TOAN_QUOC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WorkType workType = WorkType.OFFLINE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplyMode applyMode;

    private String externalLink;

    @Column(columnDefinition = "jsonb")
    private String internalForm; // cấu hình form nộp nội bộ

    @Column(nullable = false)
    private Instant deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OppStatus status = OppStatus.DRAFT;

    @Column(columnDefinition = "text")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderated_by")
    private User moderatedBy;

    private Instant moderatedAt;

    @Builder.Default
    private boolean isFeatured = false;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "featured_by")
    private User featuredBy;
    private Instant featuredAt;
    private Instant featuredUntil;

    @Builder.Default
    private int viewCount = 0;
    @Builder.Default
    private int bookmarkCount = 0;
    @Builder.Default
    private int applicationCount = 0;
    @Builder.Default
    private int shareCount = 0;

    private Instant publishedAt;

    @Builder.Default
    private Instant createdAt = Instant.now();
    @Builder.Default
    private Instant updatedAt = Instant.now();

    // RÀNG BUỘC: EXTERNAL bắt buộc có external_link; INTERNAL không cần
    @PrePersist
    @PreUpdate
    private void validate() {
        if (applyMode == ApplyMode.EXTERNAL && (externalLink == null || externalLink.isBlank())) {
            throw new IllegalStateException("external_link bắt buộc khi applyMode=EXTERNAL");
        }
        if (deadline != null && createdAt != null && deadline.isBefore(createdAt)) {
            throw new IllegalStateException("deadline phải sau thời điểm tạo");
        }
    }
}
