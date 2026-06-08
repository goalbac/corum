package com.corum.backend.service.calendar;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.calendar.*;
import com.corum.backend.domain.group.MemberGroupRepository;
import com.corum.backend.dto.calendar.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private static final int MAX_OCCURRENCES_PER_EVENT = 400;

    private final CalendarRepository calendarRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final CalendarGroupPermissionRepository calendarGroupPermissionRepository;
    private final MemberGroupRepository memberGroupRepository;

    @Transactional(readOnly = true)
    public List<CalendarResponse> getCalendars(Long memberId) {
        return calendarRepository.findByIsActiveTrueOrderByIdAsc().stream()
                .map(calendar -> new CalendarResponse(
                        calendar,
                        hasPermission(calendar.getId(), memberId, "READ"),
                        hasPermission(calendar.getId(), memberId, "WRITE")
                ))
                .filter(CalendarResponse::getCanRead)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getEvents(LocalDateTime start, LocalDateTime end, Long memberId) {
        List<Long> calendarIds = getReadableCalendarIds(memberId);
        if (calendarIds.isEmpty()) return List.of();

        List<CalendarEventResponse> singleEvents = calendarEventRepository.findByPeriod(calendarIds, start, end).stream()
                .filter(event -> isNone(event.getRecurrenceType()))
                .map(CalendarEventResponse::new)
                .toList();

        List<CalendarEventResponse> recurringEvents = calendarEventRepository
                .findRecurringCandidates(calendarIds, end).stream()
                .flatMap(event -> expandRecurringEvent(event, start, end).stream())
                .toList();

        return java.util.stream.Stream.concat(singleEvents.stream(), recurringEvents.stream())
                .sorted(Comparator.comparing(CalendarEventResponse::getStartAt))
                .toList();
    }

    @Transactional(readOnly = true)
    public CalendarEventResponse getEvent(Long id, Long memberId) {
        CalendarEvent event = getEventEntity(id);
        requirePermission(event.getCalendarId(), memberId, "READ");
        return new CalendarEventResponse(event);
    }

    @Transactional
    public CalendarEventResponse createEvent(CalendarEventRequest request, Long memberId) {
        requirePermission(request.getCalendarId(), memberId, "WRITE");
        CalendarEvent event = CalendarEvent.builder()
                .calendarId(request.getCalendarId())
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .isAllDay(Boolean.TRUE.equals(request.getIsAllDay()))
                .recurrenceType(normalizeRecurrence(request.getRecurrenceType()))
                .recurrenceRule(request.getRecurrenceRule())
                .createdBy(memberId)
                .build();
        return new CalendarEventResponse(calendarEventRepository.save(event));
    }

    @Transactional
    public CalendarEventResponse updateEvent(Long id, CalendarEventRequest request, Long memberId) {
        CalendarEvent event = getEventEntity(id);
        requirePermission(event.getCalendarId(), memberId, "WRITE");
        if (!event.getCalendarId().equals(request.getCalendarId())) {
            requirePermission(request.getCalendarId(), memberId, "WRITE");
        }
        event.update(
                request.getTitle(), request.getDescription(), request.getLocation(),
                request.getStartAt(), request.getEndAt(), Boolean.TRUE.equals(request.getIsAllDay()),
                normalizeRecurrence(request.getRecurrenceType()), request.getRecurrenceRule(), memberId
        );
        return new CalendarEventResponse(event);
    }

    @Transactional
    public void deleteEvent(Long id, Long memberId) {
        CalendarEvent event = getEventEntity(id);
        requirePermission(event.getCalendarId(), memberId, "WRITE");
        calendarEventRepository.delete(event);
    }

    @Transactional
    public CalendarResponse createCalendar(String name, String color, String description, Long memberId) {
        requireAdmin(memberId);
        CalendarEntity saved = calendarRepository.save(CalendarEntity.builder()
                .name(name).color(color).description(description).build());
        return new CalendarResponse(saved, true, true);
    }

    @Transactional(readOnly = true)
    public List<CalendarPermissionResponse> getPermissions(Long calendarId, Long memberId) {
        requireAdmin(memberId);
        ensureCalendar(calendarId);
        return calendarGroupPermissionRepository.findByCalendarId(calendarId).stream()
                .map(CalendarPermissionResponse::new)
                .toList();
    }

    @Transactional
    public List<CalendarPermissionResponse> updatePermissions(
            Long calendarId, CalendarPermissionRequest request, Long memberId) {
        requireAdmin(memberId);
        ensureCalendar(calendarId);
        calendarGroupPermissionRepository.deleteByCalendarId(calendarId);
        List<CalendarGroupPermission> permissions = request.getPermissions() == null ? List.of() :
                request.getPermissions().stream()
                        .map(item -> CalendarGroupPermission.builder()
                                .calendarId(calendarId)
                                .groupId(item.getGroupId())
                                .canRead(Boolean.TRUE.equals(item.getCanRead()))
                                .canWrite(Boolean.TRUE.equals(item.getCanWrite()))
                                .build())
                        .toList();
        return calendarGroupPermissionRepository.saveAll(permissions).stream()
                .map(CalendarPermissionResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(Long calendarId, Long memberId, String permType) {
        List<CalendarGroupPermission> allPerms = calendarGroupPermissionRepository.findByCalendarId(calendarId);
        if (memberId != null && memberGroupRepository.existsAdminGroupByMemberId(memberId)) {
            return true;
        }
        if (allPerms.isEmpty()) {
            return switch (permType) {
                case "READ" -> true;
                case "WRITE" -> memberId != null;
                default -> false;
            };
        }
        if (memberId == null) return false;
        List<Long> groupIds = memberGroupRepository.findGroupIdsByMemberId(memberId);
        if (groupIds.isEmpty()) return false;
        List<CalendarGroupPermission> perms =
                calendarGroupPermissionRepository.findByCalendarIdAndGroupIds(calendarId, groupIds);
        return perms.stream().anyMatch(p -> switch (permType) {
            case "READ" -> p.getCanRead();
            case "WRITE" -> p.getCanWrite();
            default -> false;
        });
    }

    private List<Long> getReadableCalendarIds(Long memberId) {
        return calendarRepository.findByIsActiveTrueOrderByIdAsc().stream()
                .filter(calendar -> hasPermission(calendar.getId(), memberId, "READ"))
                .map(CalendarEntity::getId)
                .toList();
    }

    private List<CalendarEventResponse> expandRecurringEvent(
            CalendarEvent event, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        String type = normalizeRecurrence(event.getRecurrenceType());
        if (isNone(type)) return List.of();

        Duration duration = Duration.between(event.getStartAt(),
                event.getEndAt() == null ? event.getStartAt() : event.getEndAt());
        List<CalendarEventResponse> occurrences = new java.util.ArrayList<>();
        LocalDateTime cursor = event.getStartAt();
        int guard = 0;
        while (!cursor.isAfter(rangeEnd) && guard < MAX_OCCURRENCES_PER_EVENT) {
            LocalDateTime occurrenceEnd = cursor.plus(duration);
            if (!cursor.isAfter(rangeEnd) && !occurrenceEnd.isBefore(rangeStart)) {
                occurrences.add(new CalendarEventResponse(event, event.getId(), cursor, occurrenceEnd));
            }
            cursor = nextOccurrence(cursor, type);
            guard++;
        }
        return occurrences;
    }

    private LocalDateTime nextOccurrence(LocalDateTime value, String recurrenceType) {
        return switch (recurrenceType) {
            case "DAILY" -> value.plusDays(1);
            case "WEEKLY" -> value.plusWeeks(1);
            case "MONTHLY" -> value.plusMonths(1);
            default -> value.plusYears(100);
        };
    }

    private String normalizeRecurrence(String value) {
        if (value == null || value.isBlank()) return "NONE";
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "DAILY", "WEEKLY", "MONTHLY" -> normalized;
            default -> "NONE";
        };
    }

    private boolean isNone(String recurrenceType) {
        return "NONE".equals(normalizeRecurrence(recurrenceType));
    }

    private CalendarEvent getEventEntity(Long id) {
        return calendarEventRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("일정을 찾을 수 없습니다."));
    }

    private void ensureCalendar(Long calendarId) {
        if (!calendarRepository.existsById(calendarId)) {
            throw BusinessException.notFound("캘린더를 찾을 수 없습니다.");
        }
    }

    private void requirePermission(Long calendarId, Long memberId, String permType) {
        ensureCalendar(calendarId);
        if (!hasPermission(calendarId, memberId, permType)) {
            throw BusinessException.forbidden("캘린더 권한이 없습니다.");
        }
    }

    private void requireAdmin(Long memberId) {
        if (memberId == null || !memberGroupRepository.existsAdminGroupByMemberId(memberId)) {
            throw BusinessException.forbidden("관리자 권한이 필요합니다.");
        }
    }
}
