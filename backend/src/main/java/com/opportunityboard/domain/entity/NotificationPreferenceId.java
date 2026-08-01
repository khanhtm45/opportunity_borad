package com.opportunityboard.domain.entity;

import com.opportunityboard.domain.enums.NotifChannel;
import com.opportunityboard.domain.enums.NotifType;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class NotificationPreferenceId implements Serializable {
    private UUID user;       // khớp tên field 'user' trong entity
    private NotifType type;
    private NotifChannel channel;
}
