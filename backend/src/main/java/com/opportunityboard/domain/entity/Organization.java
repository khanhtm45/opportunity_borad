package com.opportunityboard.domain.entity;

import com.opportunityboard.domain.enums.CompanySize;
import com.opportunityboard.domain.enums.OrgVerified;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orgId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;

    @Column(nullable = false)
    private String orgName;

    private String logoUrl;
    private String website;
    private String contactEmail;
    private String contactPhone;

    @Column(name = "tax_code", length = 40)
    private String taxCode;

    @Column(length = 500)
    private String address;

    @Column(length = 200)
    private String industry;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_size")
    private CompanySize companySize;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrgVerified verifiedStatus = OrgVerified.PENDING;

    private Instant verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    /** Phản hồi AI/Admin khi hồ sơ sai/thiếu — provider cần cập nhật theo ghi chú này. */
    @Column(name = "verification_note", columnDefinition = "text")
    private String verificationNote;

    @Column(name = "ai_scanned_at")
    private Instant aiScannedAt;

    @Builder.Default
    private Instant createdAt = Instant.now();
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
