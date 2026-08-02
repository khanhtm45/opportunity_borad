package com.opportunityboard.domain.entity;

import com.opportunityboard.domain.enums.OrgDocType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "org_documents",
       indexes = @Index(name = "idx_org_documents_org", columnList = "org_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrgDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID docId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization org;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false)
    private OrgDocType docType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "file_url", nullable = false, length = 512)
    private String fileUrl;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
