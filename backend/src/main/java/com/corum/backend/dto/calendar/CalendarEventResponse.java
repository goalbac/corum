package com.corum.backend.dto.calendar;

import com.corum.backend.domain.calendar.CalendarEvent;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CalendarEventResponse {

    private final Long id;
    private final Long originalEventId;
    private final Long calendarId;
    private final String title;
    private final String description;
    private final String location;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final Boolean isAllDay;
    private final String recurrenceType;
    private final String recurrenceRule;
    private final Long createdBy;
    private final Long updatedBy;

    public CalendarEventResponse(CalendarEvent event) {
        this(event, event.getId(), event.getStartAt(), event.getEndAt());
    }

    public CalendarEventResponse(CalendarEvent event, Long originalEventId,
                                 LocalDateTime startAt, LocalDateTime endAt) {
        this.id = event.getId();
        this.originalEventId = originalEventId;
        this.calendarId = event.getCalendarId();
        this.title = event.getTitle();
        this.description = event.getDescription();
        this.location = event.getLocation();
        this.startAt = startAt;
        this.endAt = endAt;
        this.isAllDay = event.getIsAllDay();
        this.recurrenceType = event.getRecurrenceType();
        this.recurrenceRule = event.getRecurrenceRule();
        this.createdBy = event.getCreatedBy();
        this.updatedBy = event.getUpdatedBy();
    }
}
