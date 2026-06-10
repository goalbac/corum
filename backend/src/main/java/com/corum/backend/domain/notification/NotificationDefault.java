package com.corum.backend.domain.notification;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_defaults")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NotificationDefault {

    @Id
    @Column(name = "notif_type", length = 60)
    private String notifType;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
