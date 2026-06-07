package com.corum.backend.controller.calendar;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.domain.calendar.CalendarEntity;
import com.corum.backend.domain.calendar.CalendarEvent;
import com.corum.backend.dto.calendar.CalendarEventRequest;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.calendar.CalendarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendars")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    // 캘린더 목록
    @GetMapping
    public ApiResponse<List<CalendarEntity>> getCalendars() {
        return ApiResponse.ok(calendarService.getCalendars());
    }

    // 캘린더 생성 (관리자)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CalendarEntity> createCalendar(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(calendarService.createCalendar(
                body.get("name"), body.get("color"), body.get("description")));
    }

    // 기간별 일정 조회
    @GetMapping("/events")
    public ApiResponse<List<CalendarEvent>> getEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ApiResponse.ok(calendarService.getEvents(start, end));
    }

    // 일정 단건 조회
    @GetMapping("/events/{id}")
    public ApiResponse<CalendarEvent> getEvent(@PathVariable Long id) {
        return ApiResponse.ok(calendarService.getEvent(id));
    }

    // 일정 등록
    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CalendarEvent> createEvent(
            @Valid @RequestBody CalendarEventRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(calendarService.createEvent(request, userDetails.getMemberId()));
    }

    // 일정 수정
    @PutMapping("/events/{id}")
    public ApiResponse<CalendarEvent> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody CalendarEventRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(calendarService.updateEvent(id, request, userDetails.getMemberId()));
    }

    // 일정 삭제
    @DeleteMapping("/events/{id}")
    public ApiResponse<Void> deleteEvent(@PathVariable Long id) {
        calendarService.deleteEvent(id);
        return ApiResponse.ok("일정이 삭제되었습니다.");
    }
}
