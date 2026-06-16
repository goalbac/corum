package com.corum.backend.dto.calendar;

import lombok.Getter;

import java.util.List;

@Getter
public class CalendarCreateRequest {

    private String name;
    private String color;
    private String description;
    private String calendarType = "GENERAL";
    private Boolean isActive = true;
    private List<PermissionDto> permissions;

    @Getter
    public static class PermissionDto {
        private Long groupId;
        private Boolean canRead;
        private Boolean canWrite;
    }
}
