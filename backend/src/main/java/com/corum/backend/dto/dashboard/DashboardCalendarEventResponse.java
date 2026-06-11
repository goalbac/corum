package com.corum.backend.dto.dashboard;

import com.corum.backend.domain.calendar.CalendarEntity;
import com.corum.backend.domain.calendar.CalendarEvent;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DashboardCalendarEventResponse {

    private final Long id;
    private final String title;
    private final String location;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final Boolean isAllDay;
    private final Long calendarId;
    private final String calendarName;
    private final String calendarColor;

    public DashboardCalendarEventResponse(CalendarEvent event, CalendarEntity calendar) {
        this.id            = event.getId();
        this.title         = event.getTitle();
        this.location      = event.getLocation();
        this.startAt       = event.getStartAt();
        this.endAt         = event.getEndAt();
        this.isAllDay      = event.getIsAllDay();
        this.calendarId    = event.getCalendarId();
        this.calendarName  = calendar != null ? calendar.getName()  : null;
        this.calendarColor = calendar != null ? calendar.getColor() : null;
    }
}
