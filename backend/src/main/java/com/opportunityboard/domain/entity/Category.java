package com.opportunityboard.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID categoryId;

    @Column(nullable = false, unique = true)
    private String code; // INTERNSHIP, HACKATHON...

    @Column(nullable = false)
    private String categoryName;

    @Builder.Default
    private int displayOrder = 0;

    @Builder.Default
    private boolean isSystem = true; // TRUE = cố định, không xóa

    @Builder.Default
    private Instant createdAt = Instant.now();
}
