package com.corum.backend.dto.calendar;

import com.corum.backend.common.FlexibleLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CalendarEventRequest {

    @NotNull(message = "캘린더를 선택해주세요.")
    private Long calendarId;

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    private String description;
    private String location;

    @NotNull(message = "시작 시간을 입력해주세요.")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime startAt;

    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime endAt;

    private Boolean isAllDay = false;

    private String recurrenceType = "NONE";

    private String recurrenceRule;
}
