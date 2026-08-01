package com.opportunityboard.domain.entity;

import com.opportunityboard.domain.enums.NotifChannel;
import com.opportunityboard.domain.enums.NotifType;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotifType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotifChannel channel;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String body;

    private UUID refId; // opp_id / app_id

    @Builder.Default
    private boolean isRead = false;
    private Instant sentAt;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
