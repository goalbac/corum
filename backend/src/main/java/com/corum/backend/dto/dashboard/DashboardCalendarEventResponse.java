package com.corum.backend.dto.dashboard;

import com.corum.backend.domain.calendar.CalendarEntity;
import com.corum.backend.domain.calendar.CalendarEvent;
import com.corum.backend.dto.calendar.CalendarEventDto;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DashboardCalendarEventResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final String location;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final Boolean isAllDay;
    private final Long calendarId;
    private final String calendarName;
    private final String calendarColor;
    private final String calendarType;

    public DashboardCalendarEventResponse(CalendarEvent event, CalendarEntity calendar) {
        this.id            = event.getId();
        this.title         = event.getTitle();
        this.description   = event.getDescription();
        this.location      = event.getLocation();
        this.startAt       = event.getStartAt();
        this.endAt         = event.getEndAt();
        this.isAllDay      = event.getIsAllDay();
        this.calendarId    = event.getCalendarId();
        this.calendarName  = calendar != null ? calendar.getName()      : null;
        this.calendarColor = calendar != null ? calendar.getColor()     : null;
        this.calendarType  = calendar != null ? calendar.getCalendarType() : null;
    }

    public DashboardCalendarEventResponse(CalendarEventDto dto, CalendarEntity calendar) {
        this.id            = dto.getId();
        this.title         = dto.getTitle();
        this.description   = dto.getDescription();
        this.location      = dto.getLocation();
        this.startAt       = dto.getStartAt();
        this.endAt         = dto.getEndAt();
        this.isAllDay      = dto.getIsAllDay();
        this.calendarId    = dto.getCalendarId();
        this.calendarName  = calendar != null ? calendar.getName()      : null;
        this.calendarColor = calendar != null ? calendar.getColor()     : null;
        this.calendarType  = calendar != null ? calendar.getCalendarType() : null;
    }
}
