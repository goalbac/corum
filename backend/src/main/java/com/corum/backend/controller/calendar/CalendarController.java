package com.corum.backend.controller.calendar;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.calendar.*;
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

    @GetMapping
    public ApiResponse<List<CalendarResponse>> getCalendars(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(calendarService.getCalendars(memberId(userDetails)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CalendarResponse> createCalendar(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(calendarService.createCalendar(
                body.get("name"), body.get("color"), body.get("description"), memberId(userDetails)));
    }

    @GetMapping("/events")
    public ApiResponse<List<CalendarEventResponse>> getEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(calendarService.getEvents(start, end, memberId(userDetails)));
    }

    @GetMapping("/events/{id}")
    public ApiResponse<CalendarEventResponse> getEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(calendarService.getEvent(id, memberId(userDetails)));
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CalendarEventResponse> createEvent(
            @Valid @RequestBody CalendarEventRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(calendarService.createEvent(request, memberId(userDetails)));
    }

    @PutMapping("/events/{id}")
    public ApiResponse<CalendarEventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody CalendarEventRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(calendarService.updateEvent(id, request, memberId(userDetails)));
    }

    @DeleteMapping("/events/{id}")
    public ApiResponse<Void> deleteEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        calendarService.deleteEvent(id, memberId(userDetails));
        return ApiResponse.ok("일정이 삭제되었습니다.");
    }

    @GetMapping("/{calendarId}/permissions")
    public ApiResponse<List<CalendarPermissionResponse>> getPermissions(
            @PathVariable Long calendarId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(calendarService.getPermissions(calendarId, memberId(userDetails)));
    }

    @PutMapping("/{calendarId}/permissions")
    public ApiResponse<List<CalendarPermissionResponse>> updatePermissions(
            @PathVariable Long calendarId,
            @Valid @RequestBody CalendarPermissionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(calendarService.updatePermissions(calendarId, request, memberId(userDetails)));
    }

    private Long memberId(CustomUserDetails userDetails) {
        return userDetails == null ? null : userDetails.getMemberId();
    }
}
