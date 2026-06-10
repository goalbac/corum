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

    @Column(name = "system_enabled", nullable = false)
    private Boolean systemEnabled;

    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    public void setSystemEnabled(Boolean v) { this.systemEnabled = v; }
    public void setEmailEnabled(Boolean v)  { this.emailEnabled  = v; }
}
