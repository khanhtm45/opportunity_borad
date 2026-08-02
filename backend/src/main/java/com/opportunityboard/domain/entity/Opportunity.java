package com.opportunityboard.domain.entity;

import com.opportunityboard.domain.enums.*;
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

    @Column(name = "salary_min")
    private Long salaryMin;

    @Column(name = "salary_max")
    private Long salaryMax;

    @Column(name = "salary_currency", length = 10)
    @Builder.Default
    private String salaryCurrency = "VND";

    @Column(name = "salary_negotiable", nullable = false)
    @Builder.Default
    private boolean salaryNegotiable = false;

    @Column(name = "selection_process", columnDefinition = "text")
    private String selectionProcess;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_level")
    private JobLevel jobLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level")
    private ExperienceLevel experienceLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "education_level")
    private EducationLevel educationLevel;

    private Integer headcount;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type")
    private EmploymentType employmentType;

    @Column(name = "address_detail", length = 500)
    private String addressDetail;

    @Column(name = "working_schedule", columnDefinition = "text")
    private String workingSchedule;

    @Column(columnDefinition = "text")
    private String skills; // CSV hoặc JSON text kỹ năng

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

    @Column(name = "external_ref")
    private String externalRef; // mã tham chiếu bên thứ 3 ("case index ngoài")

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

    /** Ghi chú AI/Admin khi yêu cầu bổ sung hồ sơ / sửa tin (provider xem trên portal). */
    @Column(name = "ai_moderation_note", columnDefinition = "text")
    private String aiModerationNote;

    @Column(name = "ai_scanned_at")
    private Instant aiScannedAt;

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
