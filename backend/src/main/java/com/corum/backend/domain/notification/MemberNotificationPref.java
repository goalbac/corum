package com.corum.backend.domain.notification;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_notification_prefs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class MemberNotificationPref {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "notif_type", nullable = false, length = 60)
    private String notifType;

    @Column(name = "system_enabled", nullable = false)
    @Builder.Default
    private Boolean systemEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private Boolean emailEnabled = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void setSystemEnabled(Boolean v) { this.systemEnabled = v; }
    public void setEmailEnabled(Boolean v)  { this.emailEnabled  = v; }
}
