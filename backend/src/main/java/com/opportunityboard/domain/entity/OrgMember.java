package com.opportunityboard.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "org_members")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrgMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orgMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization org;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    private String memberRole = "RECRUITER"; // OWNER/RECRUITER/MEMBER

    @Builder.Default
    private Instant createdAt = Instant.now();
}
