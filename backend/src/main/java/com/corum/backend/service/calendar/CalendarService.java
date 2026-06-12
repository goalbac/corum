package com.corum.backend.service.calendar;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.calendar.*;
import com.corum.backend.domain.group.MemberGroup;
import com.corum.backend.domain.group.MemberGroupRepository;
import com.corum.backend.dto.calendar.CalendarCreateRequest;
import com.corum.backend.dto.calendar.CalendarEventRequest;
import com.corum.backend.dto.calendar.CalendarResponse;
import com.corum.backend.service.notification.NotificationService;
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
    private final CalendarGroupPermissionRepository permissionRepository;
    private final MemberGroupRepository memberGroupRepository;
    private final NotificationService notificationService;

    // ===== 캘린더 목록 (활성) =====
    @Transactional(readOnly = true)
    public List<CalendarResponse> getCalendars() {
        return calendarRepository.findByIsActiveTrueOrderByIdAsc().stream()
                .map(c -> new CalendarResponse(c, permissionRepository.findByCalendarId(c.getId())))
                .collect(Collectors.toList());
    }

    // ===== 관리자용 전체 캘린더 =====
    @Transactional(readOnly = true)
    public List<CalendarResponse> getAllCalendars() {
        return calendarRepository.findAll().stream()
                .map(c -> new CalendarResponse(c, permissionRepository.findByCalendarId(c.getId())))
                .collect(Collectors.toList());
    }

    // ===== 읽기 가능한 캘린더 ID 목록 =====
    private List<Long> getReadableCalendarIds(Long memberId) {
        List<Long> allActive = calendarRepository.findByIsActiveTrueOrderByIdAsc()
                .stream().map(CalendarEntity::getId).collect(Collectors.toList());

        if (memberId == null) {
            // 비로그인: 권한 설정 없는 캘린더만 접근 (공개)
            return allActive.stream()
                    .filter(id -> permissionRepository.findByCalendarId(id).isEmpty())
                    .collect(Collectors.toList());
        }
        if (isSuperAdmin(memberId)) {
            return allActive;
        }
        List<Long> groupIds = memberGroupRepository.findGroupIdsByMemberId(memberId);
        if (groupIds.isEmpty()) {
            return allActive.stream()
                    .filter(id -> permissionRepository.findByCalendarId(id).isEmpty())
                    .collect(Collectors.toList());
        }
        // 권한 없는 캘린더(공개) + 읽기 가능한 캘린더
        List<Long> readable = permissionRepository.findReadableCalendarIds(groupIds);
        List<Long> publicIds = allActive.stream()
                .filter(id -> permissionRepository.findByCalendarId(id).isEmpty())
                .collect(Collectors.toList());
        return allActive.stream()
                .filter(id -> readable.contains(id) || publicIds.contains(id))
                .collect(Collectors.toList());
    }

    // ===== 쓰기 권한 체크 =====
    public boolean canWrite(Long calendarId, Long memberId) {
        if (memberId == null) return false;
        if (isSuperAdmin(memberId)) return true;
        List<Long> perms = permissionRepository.findByCalendarId(calendarId).stream()
                .map(p -> p.getGroupId()).collect(Collectors.toList());
        if (perms.isEmpty()) return true; // 공개 캘린더
        List<Long> groupIds = memberGroupRepository.findGroupIdsByMemberId(memberId);
        if (groupIds.isEmpty()) return false;
        return permissionRepository.canWrite(calendarId, groupIds);
    }

    private boolean isSuperAdmin(Long memberId) {
        return memberId != null && memberGroupRepository.existsSuperAdminGroupByMemberId(memberId);
    }

    // ===== 기간별 일정 조회 (권한 필터) =====
    @Transactional(readOnly = true)
    public List<CalendarEvent> getEvents(LocalDateTime start, LocalDateTime end, Long memberId, List<Long> requestedIds) {
        List<Long> readable = getReadableCalendarIds(memberId);
        if (readable.isEmpty()) return List.of();
        // 요청된 캘린더 ID가 있으면 readable과 교집합, 없으면 readable 전체
        List<Long> calendarIds = (requestedIds == null || requestedIds.isEmpty())
                ? readable
                : readable.stream().filter(requestedIds::contains).collect(Collectors.toList());
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
        if (!canWrite(request.getCalendarId(), memberId)) {
            throw BusinessException.forbidden("이 캘린더에 일정을 추가할 권한이 없습니다.");
        }
        CalendarEvent event = CalendarEvent.builder()
                .calendarId(request.getCalendarId())
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .isAllDay(request.getIsAllDay() != null ? request.getIsAllDay() : false)
                .recurrenceType(request.getRecurrenceType() != null ? request.getRecurrenceType() : "NONE")
                .recurrenceRule(request.getRecurrenceRule())
                .createdBy(memberId)
                .build();
        CalendarEvent saved = calendarEventRepository.save(event);

        // 이 캘린더에 read 권한이 있는 회원들에게 알림 (등록자 제외)
        try {
            List<Long> readGroupIds = permissionRepository.findByCalendarId(request.getCalendarId()).stream()
                    .filter(p -> Boolean.TRUE.equals(p.getCanRead()))
                    .map(CalendarGroupPermission::getGroupId)
                    .collect(Collectors.toList());
            List<Long> recipientIds = readGroupIds.isEmpty() ? List.of()
                    : memberGroupRepository.findByGroupIdIn(readGroupIds).stream()
                            .map(MemberGroup::getMemberId)
                            .filter(id -> !id.equals(memberId))
                            .distinct()
                            .collect(Collectors.toList());
            if (!recipientIds.isEmpty()) {
                String title = saved.getTitle();
                notificationService.createForMembers(
                        recipientIds, "CALENDAR_EVENT",
                        "새 일정: " + (title.length() > 30 ? title.substring(0, 30) + "…" : title),
                        null, "/calendar"
                );
            }
        } catch (Exception ignored) { }

        return saved;
    }

    // ===== 일정 수정 =====
    @Transactional
    public CalendarEvent updateEvent(Long id, CalendarEventRequest request, Long memberId) {
        CalendarEvent event = calendarEventRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("일정을 찾을 수 없습니다."));
        if (!event.getCreatedBy().equals(memberId) && !canWrite(event.getCalendarId(), memberId)) {
            throw BusinessException.forbidden("수정 권한이 없습니다.");
        }
        event.update(
                request.getTitle(), request.getDescription(), request.getLocation(),
                request.getStartAt(), request.getEndAt(),
                request.getIsAllDay() != null ? request.getIsAllDay() : false,
                request.getRecurrenceType() != null ? request.getRecurrenceType() : "NONE",
                request.getRecurrenceRule(), memberId
        );
        return event;
    }

    // ===== 일정 삭제 =====
    @Transactional
    public void deleteEvent(Long id, Long memberId) {
        CalendarEvent event = calendarEventRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("일정을 찾을 수 없습니다."));
        if (!event.getCreatedBy().equals(memberId) && !canWrite(event.getCalendarId(), memberId)) {
            throw BusinessException.forbidden("삭제 권한이 없습니다.");
        }
        calendarEventRepository.deleteById(id);
    }

    // ===== 캘린더 생성 =====
    @Transactional
    public CalendarResponse createCalendar(CalendarCreateRequest request) {
        CalendarEntity calendar = calendarRepository.save(CalendarEntity.builder()
                .name(request.getName())
                .color(request.getColor() != null ? request.getColor() : "#2563EB")
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build());
        savePermissions(calendar.getId(), request.getPermissions());
        return new CalendarResponse(calendar, permissionRepository.findByCalendarId(calendar.getId()));
    }

    // ===== 캘린더 수정 =====
    @Transactional
    public CalendarResponse updateCalendar(Long id, CalendarCreateRequest request) {
        CalendarEntity calendar = calendarRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("캘린더를 찾을 수 없습니다."));
        calendar.update(request.getName(), request.getColor(), request.getDescription());
        if (request.getIsActive() != null) {
            calendar.updateActive(request.getIsActive());
        }
        permissionRepository.deleteByCalendarId(id);
        savePermissions(id, request.getPermissions());
        return new CalendarResponse(calendar, permissionRepository.findByCalendarId(id));
    }

    // ===== 캘린더 삭제 =====
    @Transactional
    public void deleteCalendar(Long id) {
        if (!calendarRepository.existsById(id)) {
            throw BusinessException.notFound("캘린더를 찾을 수 없습니다.");
        }
        permissionRepository.deleteByCalendarId(id);
        calendarEventRepository.deleteByCalendarId(id);
        calendarRepository.deleteById(id);
    }

    private void savePermissions(Long calendarId, List<CalendarCreateRequest.PermissionDto> perms) {
        if (perms == null || perms.isEmpty()) return;
        perms.stream().filter(p -> p.getGroupId() != null).forEach(p -> permissionRepository.save(CalendarGroupPermission.builder()
                .calendarId(calendarId)
                .groupId(p.getGroupId())
                .canRead(p.getCanRead() != null ? p.getCanRead() : true)
                .canWrite(p.getCanWrite() != null ? p.getCanWrite() : false)
                .build()));
    }
}
