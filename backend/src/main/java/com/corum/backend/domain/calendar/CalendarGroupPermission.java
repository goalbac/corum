package com.corum.backend.domain.calendar;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "calendar_group_permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class CalendarGroupPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "calendar_id", nullable = false)
    private Long calendarId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "can_read", nullable = false)
    @Builder.Default
    private Boolean canRead = true;

    @Column(name = "can_write", nullable = false)
    @Builder.Default
    private Boolean canWrite = false;
}
