package com.corum.backend.controller.calendar;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.domain.calendar.CalendarEvent;
import com.corum.backend.dto.calendar.CalendarCreateRequest;
import com.corum.backend.dto.calendar.CalendarEventRequest;
import com.corum.backend.dto.calendar.CalendarResponse;
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

@RestController
@RequestMapping("/api/calendars")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    // ===== 캘린더 목록 (활성) =====
    @GetMapping
    public ApiResponse<List<CalendarResponse>> getCalendars() {
        return ApiResponse.ok(calendarService.getCalendars());
    }

    // ===== 관리자용 전체 캘린더 목록 =====
    @GetMapping("/admin")
    public ApiResponse<List<CalendarResponse>> getAllCalendars() {
        return ApiResponse.ok(calendarService.getAllCalendars());
    }

    // ===== 캘린더 생성 (관리자) =====
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CalendarResponse> createCalendar(@RequestBody CalendarCreateRequest request) {
        return ApiResponse.ok(calendarService.createCalendar(request));
    }

    // ===== 캘린더 수정 (관리자) =====
    @PutMapping("/{id}")
    public ApiResponse<CalendarResponse> updateCalendar(
            @PathVariable Long id,
            @RequestBody CalendarCreateRequest request) {
        return ApiResponse.ok(calendarService.updateCalendar(id, request));
    }

    // ===== 캘린더 삭제 (관리자) =====
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCalendar(@PathVariable Long id) {
        calendarService.deleteCalendar(id);
        return ApiResponse.ok("캘린더가 삭제되었습니다.");
    }

    // ===== 기간별 일정 조회 (권한 필터) =====
    @GetMapping("/events")
    public ApiResponse<List<CalendarEvent>> getEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        return ApiResponse.ok(calendarService.getEvents(start, end, memberId));
    }

    // ===== 일정 단건 조회 =====
    @GetMapping("/events/{id}")
    public ApiResponse<CalendarEvent> getEvent(@PathVariable Long id) {
        return ApiResponse.ok(calendarService.getEvent(id));
    }

    // ===== 일정 등록 =====
    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CalendarEvent> createEvent(
            @Valid @RequestBody CalendarEventRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        return ApiResponse.ok(calendarService.createEvent(request, memberId));
    }

    // ===== 일정 수정 =====
    @PutMapping("/events/{id}")
    public ApiResponse<CalendarEvent> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody CalendarEventRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        return ApiResponse.ok(calendarService.updateEvent(id, request, memberId));
    }

    // ===== 일정 삭제 =====
    @DeleteMapping("/events/{id}")
    public ApiResponse<Void> deleteEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        calendarService.deleteEvent(id, memberId);
        return ApiResponse.ok("일정이 삭제되었습니다.");
    }
}
