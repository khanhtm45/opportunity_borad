package com.opportunityboard.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "domains")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Domain {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID domainId;

    @Column(nullable = false, unique = true)
    private String domainName; // IT, Marketing, Finance...

    @Builder.Default
    private Instant createdAt = Instant.now();
}
