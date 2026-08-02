package com.opportunityboard.domain.entity;

import com.opportunityboard.domain.enums.OppDocType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "opportunity_documents",
       indexes = @Index(name = "idx_opp_documents_opp", columnList = "opp_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OpportunityDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID docId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opp_id", nullable = false)
    private Opportunity opportunity;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false)
    private OppDocType docType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "file_url", nullable = false, length = 512)
    private String fileUrl;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
