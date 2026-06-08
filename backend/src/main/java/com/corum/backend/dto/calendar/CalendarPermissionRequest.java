package com.corum.backend.dto.calendar;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CalendarPermissionRequest {

    @Valid
    private List<Item> permissions;

    @Getter
    @Setter
    public static class Item {
        @NotNull
        private Long groupId;
        private Boolean canRead = true;
        private Boolean canWrite = false;
    }
}
