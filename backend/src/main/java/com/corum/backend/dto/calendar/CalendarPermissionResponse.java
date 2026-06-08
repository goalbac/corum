package com.corum.backend.dto.calendar;

import com.corum.backend.domain.calendar.CalendarGroupPermission;
import lombok.Getter;

@Getter
public class CalendarPermissionResponse {

    private final Long id;
    private final Long calendarId;
    private final Long groupId;
    private final Boolean canRead;
    private final Boolean canWrite;

    public CalendarPermissionResponse(CalendarGroupPermission permission) {
        this.id = permission.getId();
        this.calendarId = permission.getCalendarId();
        this.groupId = permission.getGroupId();
        this.canRead = permission.getCanRead();
        this.canWrite = permission.getCanWrite();
    }
}
