package com.corum.backend.domain.calendar;

import com.corum.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "calendar_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class CalendarEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "calendar_id", nullable = false)
    private Long calendarId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "location", length = 500)
    private String location;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "is_all_day", nullable = false)
    @Builder.Default
    private Boolean isAllDay = false;

    // NONE, DAILY, WEEKLY, MONTHLY
    @Column(name = "recurrence_type", nullable = false, length = 20)
    @Builder.Default
    private String recurrenceType = "NONE";

    @Column(name = "recurrence_rule", columnDefinition = "TEXT")
    private String recurrenceRule;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    public void update(String title, String description, String location,
                       LocalDateTime startAt, LocalDateTime endAt,
                       Boolean isAllDay, String recurrenceType,
                       String recurrenceRule, Long updatedBy) {
        this.title          = title;
        this.description    = description;
        this.location       = location;
        this.startAt        = startAt;
        this.endAt          = endAt;
        this.isAllDay       = isAllDay;
        this.recurrenceType = recurrenceType;
        this.recurrenceRule = recurrenceRule;
        this.updatedBy      = updatedBy;
    }
}
