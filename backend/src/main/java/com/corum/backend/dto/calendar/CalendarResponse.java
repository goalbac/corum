package com.corum.backend.dto.calendar;

import com.corum.backend.domain.calendar.CalendarEntity;
import lombok.Getter;

@Getter
public class CalendarResponse {

    private final Long id;
    private final String name;
    private final String color;
    private final String description;
    private final Boolean isActive;
    private final Boolean canRead;
    private final Boolean canWrite;

    public CalendarResponse(CalendarEntity calendar, boolean canRead, boolean canWrite) {
        this.id = calendar.getId();
        this.name = calendar.getName();
        this.color = calendar.getColor();
        this.description = calendar.getDescription();
        this.isActive = calendar.getIsActive();
        this.canRead = canRead;
        this.canWrite = canWrite;
    }
}
