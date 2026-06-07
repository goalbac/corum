package com.corum.backend.service.calendar;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.calendar.*;
import com.corum.backend.dto.calendar.CalendarEventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarRepository calendarRepository;
    private final CalendarEventRepository calendarEventRepository;

    // ===== 캘린더 목록 =====
    @Transactional(readOnly = true)
    public List<CalendarEntity> getCalendars() {
        return calendarRepository.findByIsActiveTrueOrderByIdAsc();
    }

    // ===== 기간별 일정 조회 =====
    @Transactional(readOnly = true)
    public List<CalendarEvent> getEvents(LocalDateTime start, LocalDateTime end) {
        List<Long> calendarIds = calendarRepository.findByIsActiveTrueOrderByIdAsc()
                .stream().map(CalendarEntity::getId).collect(Collectors.toList());
        if (calendarIds.isEmpty()) return List.of();
        return calendarEventRepository.findByPeriod(calendarIds, start, end);
    }

    // ===== 일정 단건 조회 =====
    @Transactional(readOnly = true)
    public CalendarEvent getEvent(Long id) {
        return calendarEventRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("일정을 찾을 수 없습니다."));
    }

    // ===== 일정 등록 =====
    @Transactional
    public CalendarEvent createEvent(CalendarEventRequest request, Long memberId) {
        CalendarEvent event = CalendarEvent.builder()
                .calendarId(request.getCalendarId())
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .isAllDay(request.getIsAllDay())
                .recurrenceType(request.getRecurrenceType())
                .recurrenceRule(request.getRecurrenceRule())
                .createdBy(memberId)
                .build();
        return calendarEventRepository.save(event);
    }

    // ===== 일정 수정 =====
    @Transactional
    public CalendarEvent updateEvent(Long id, CalendarEventRequest request, Long memberId) {
        CalendarEvent event = calendarEventRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("일정을 찾을 수 없습니다."));
        event.update(
                request.getTitle(), request.getDescription(), request.getLocation(),
                request.getStartAt(), request.getEndAt(), request.getIsAllDay(),
                request.getRecurrenceType(), request.getRecurrenceRule(), memberId
        );
        return event;
    }

    // ===== 일정 삭제 =====
    @Transactional
    public void deleteEvent(Long id) {
        if (!calendarEventRepository.existsById(id)) {
            throw BusinessException.notFound("일정을 찾을 수 없습니다.");
        }
        calendarEventRepository.deleteById(id);
    }

    // ===== 캘린더 생성 =====
    @Transactional
    public CalendarEntity createCalendar(String name, String color, String description) {
        return calendarRepository.save(CalendarEntity.builder()
                .name(name).color(color).description(description).build());
    }
}
