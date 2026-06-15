package com.corum.backend.dto.calendar;

import com.corum.backend.domain.calendar.CalendarEntity;
import com.corum.backend.domain.calendar.CalendarGroupPermission;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class CalendarResponse {

    private final Long id;
    private final String name;
    private final String color;
    private final String description;
    private final Boolean isActive;
    private final List<PermissionDto> permissions;

    public CalendarResponse(CalendarEntity calendar, List<CalendarGroupPermission> perms) {
        this(calendar, perms, Map.of());
    }

    public CalendarResponse(CalendarEntity calendar, List<CalendarGroupPermission> perms, Map<Long, String> groupNames) {
        this.id          = calendar.getId();
        this.name        = calendar.getName();
        this.color       = calendar.getColor();
        this.description = calendar.getDescription();
        this.isActive    = calendar.getIsActive();
        this.permissions = perms.stream()
                .map(p -> new PermissionDto(p.getGroupId(), groupNames.getOrDefault(p.getGroupId(), ""),
                        p.getCanRead(), p.getCanWrite()))
                .collect(Collectors.toList());
    }

    @Getter
    public static class PermissionDto {
        private final Long groupId;
        private final String groupName;
        private final Boolean canRead;
        private final Boolean canWrite;

        public PermissionDto(Long groupId, String groupName, Boolean canRead, Boolean canWrite) {
            this.groupId   = groupId;
            this.groupName = groupName;
            this.canRead   = canRead;
            this.canWrite  = canWrite;
        }
    }
}
