package com.corum.backend.dto.calendar;

import com.corum.backend.domain.calendar.CalendarEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CalendarEventDto {
    private Long id;
    private Long calendarId;
    private String title;
    private String description;
    private String location;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Boolean isAllDay;
    private String recurrenceType;
    private String recurrenceRule;
    private Long createdBy;

    public static CalendarEventDto from(CalendarEvent e) {
        return CalendarEventDto.builder()
                .id(e.getId())
                .calendarId(e.getCalendarId())
                .title(e.getTitle())
                .description(e.getDescription())
                .location(e.getLocation())
                .startAt(e.getStartAt())
                .endAt(e.getEndAt())
                .isAllDay(e.getIsAllDay())
                .recurrenceType(e.getRecurrenceType())
                .recurrenceRule(e.getRecurrenceRule())
                .createdBy(e.getCreatedBy())
                .build();
    }

    public CalendarEventDto withTimes(LocalDateTime newStart, LocalDateTime newEnd) {
        return CalendarEventDto.builder()
                .id(this.id)
                .calendarId(this.calendarId)
                .title(this.title)
                .description(this.description)
                .location(this.location)
                .startAt(newStart)
                .endAt(newEnd)
                .isAllDay(this.isAllDay)
                .recurrenceType(this.recurrenceType)
                .recurrenceRule(this.recurrenceRule)
                .createdBy(this.createdBy)
                .build();
    }
}
