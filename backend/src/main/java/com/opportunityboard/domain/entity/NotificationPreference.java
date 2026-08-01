package com.opportunityboard.domain.entity;

import com.opportunityboard.domain.enums.NotifChannel;
import com.opportunityboard.domain.enums.NotifFrequency;
import com.opportunityboard.domain.enums.NotifType;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * PK composite = (user_id, type, channel) — khớp schema.sql (không có cột id riêng).
 */
@Entity
@Table(name = "notification_preferences",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "type", "channel"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(NotificationPreferenceId.class)
public class NotificationPreference {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotifType type;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private NotifChannel channel;

    @Builder.Default
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NotifFrequency frequency = NotifFrequency.INSTANT;

    @Column(columnDefinition = "jsonb")
    private String categories; // mảng category_id (NEW_OPP)

    @Column(columnDefinition = "jsonb")
    private String domains;     // mảng domain_id
}
