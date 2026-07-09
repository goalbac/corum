package com.corum.backend.service.calendar;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.calendar.*;
import com.corum.backend.domain.group.Group;
import com.corum.backend.domain.group.GroupRepository;
import com.corum.backend.domain.group.MemberGroup;
import com.corum.backend.domain.group.MemberGroupRepository;
import com.corum.backend.dto.calendar.CalendarCreateRequest;
import com.corum.backend.dto.calendar.CalendarEventDto;
import com.corum.backend.dto.calendar.CalendarEventRequest;
import com.corum.backend.dto.calendar.CalendarResponse;
import com.corum.backend.service.notification.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarRepository calendarRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final CalendarGroupPermissionRepository permissionRepository;
    private final MemberGroupRepository memberGroupRepository;
    private final GroupRepository groupRepository;
    private final NotificationService notificationService;

    // ===== 캘린더 목록 (활성) =====
    @Transactional(readOnly = true)
    public List<CalendarResponse> getCalendars() {
        return calendarRepository.findByIsActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(c -> new CalendarResponse(c, permissionRepository.findByCalendarId(c.getId())))
                .collect(Collectors.toList());
    }

    // ===== 관리자용 전체 캘린더 =====
    @Transactional(readOnly = true)
    public List<CalendarResponse> getAllCalendars() {
        Map<Long, String> groupNames = groupRepository.findAll().stream()
                .collect(Collectors.toMap(Group::getId, Group::getName));
        return calendarRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(c -> new CalendarResponse(c, permissionRepository.findByCalendarId(c.getId()), groupNames))
                .collect(Collectors.toList());
    }

    // ===== 캘린더 순서 변경 =====
    @Transactional
    public void reorder(List<Long> orderedIds) {
        Map<Long, CalendarEntity> map = calendarRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .collect(Collectors.toMap(CalendarEntity::getId, c -> c));
        for (int i = 0; i < orderedIds.size(); i++) {
            CalendarEntity c = map.get(orderedIds.get(i));
            if (c != null) c.updateSortOrder(i);
        }
    }

    // ===== 읽기 가능한 캘린더 ID 목록 =====
    public List<Long> getReadableCalendarIds(Long memberId) {
        List<Long> allActive = calendarRepository.findByIsActiveTrueOrderBySortOrderAscIdAsc()
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

    // ===== 기간별 일정 조회 (권한 필터 + 반복 일정 확장) =====
    @Transactional(readOnly = true)
    public List<CalendarEventDto> getEvents(LocalDateTime start, LocalDateTime end, Long memberId, List<Long> requestedIds) {
        List<Long> readable = getReadableCalendarIds(memberId);
        if (readable.isEmpty()) return List.of();
        List<Long> calendarIds = (requestedIds == null || requestedIds.isEmpty())
                ? readable
                : readable.stream().filter(requestedIds::contains).collect(Collectors.toList());
        if (calendarIds.isEmpty()) return List.of();

        // 기간 내 일반 이벤트 (반복 포함)
        List<CalendarEvent> inPeriod = calendarEventRepository.findByPeriod(calendarIds, start, end);

        // 기간보다 앞서 시작된 반복 이벤트 (기간 내 occurrence가 있을 수 있음)
        List<CalendarEvent> earlyRecurring = calendarEventRepository.findRecurringBeforeEnd(calendarIds, end)
                .stream()
                .filter(e -> e.getStartAt().isBefore(start)) // 이미 inPeriod에 포함된 것 제외
                .collect(Collectors.toList());

        List<CalendarEventDto> result = new ArrayList<>();

        // inPeriod 이벤트 처리
        Set<String> addedKeys = new HashSet<>();
        for (CalendarEvent ev : inPeriod) {
            CalendarEventDto dto = CalendarEventDto.from(ev);
            if ("NONE".equals(ev.getRecurrenceType())) {
                result.add(dto);
            } else {
                // 반복 이벤트는 기간 내 모든 occurrence 확장
                expandAndAdd(dto, start, end, result, addedKeys);
            }
        }

        // 기간 앞서 시작한 반복 이벤트 확장
        for (CalendarEvent ev : earlyRecurring) {
            CalendarEventDto dto = CalendarEventDto.from(ev);
            expandAndAdd(dto, start, end, result, addedKeys);
        }

        result.sort(Comparator.comparing(CalendarEventDto::getStartAt));
        return result;
    }

    private void expandAndAdd(CalendarEventDto dto, LocalDateTime windowStart, LocalDateTime windowEnd,
                               List<CalendarEventDto> result, Set<String> addedKeys) {
        JsonNode rule = parseRule(dto.getRecurrenceRule());
        LocalDateTime until = parseUntil(rule, windowEnd);
        int maxCount = parseCount(rule);
        int interval = parseInterval(rule);
        Duration duration = dto.getEndAt() != null
                ? Duration.between(dto.getStartAt(), dto.getEndAt()) : null;

        String type = dto.getRecurrenceType();

        // WEEKLY with days[]: 각 요일별로 occurrence 생성
        if ("WEEKLY".equals(type) && rule != null && rule.has("days") && rule.get("days").isArray()) {
            List<Integer> days = new ArrayList<>();
            rule.get("days").forEach(d -> days.add(d.asInt()));
            if (days.isEmpty()) return;

            // 시작일 기준 주의 첫날(일요일)부터 탐색
            LocalDateTime weekStart = dto.getStartAt().with(DayOfWeek.SUNDAY);
            if (weekStart.isAfter(dto.getStartAt())) weekStart = weekStart.minusWeeks(1);

            int occCount = 0;
            int weekSafety = 0;
            outer:
            while (!weekStart.isAfter(windowEnd) && !weekStart.isAfter(until) && weekSafety++ < 3000) {
                for (int dayNum : days) {
                    // dayNum: 0=Sun,1=Mon,...,6=Sat (JS getDay() 기준)
                    DayOfWeek dow = dayNumToDayOfWeek(dayNum);
                    LocalDateTime occStart = weekStart.with(TemporalAdjusters.nextOrSame(dow))
                            .withHour(dto.getStartAt().getHour())
                            .withMinute(dto.getStartAt().getMinute())
                            .withSecond(dto.getStartAt().getSecond());
                    if (occStart.isBefore(dto.getStartAt()) || occStart.isAfter(until) || occStart.isAfter(windowEnd)) continue;
                    if (occCount >= maxCount) break outer;
                    LocalDateTime occEnd = duration != null ? occStart.plus(duration) : null;
                    if (!occStart.isBefore(windowStart) || (occEnd != null && !occEnd.isBefore(windowStart))) {
                        String key = dto.getId() + "_" + occStart;
                        if (addedKeys.add(key)) {
                            result.add(dto.withTimes(occStart, occEnd));
                            occCount++;
                        }
                    }
                }
                weekStart = weekStart.plusWeeks(interval);
            }
            return;
        }

        // MONTHLY with monthlyType=WEEKDAY: N번째 X요일
        if ("MONTHLY".equals(type) && rule != null && "WEEKDAY".equals(rule.path("monthlyType").asText(""))) {
            LocalDateTime occStart = dto.getStartAt();
            int occCount = 0;
            int safety = 0;
            while (!occStart.isAfter(windowEnd) && !occStart.isAfter(until) && safety++ < 3000 && occCount < maxCount) {
                LocalDateTime occEnd = duration != null ? occStart.plus(duration) : null;
                if (!occStart.isBefore(windowStart) || (occEnd != null && !occEnd.isBefore(windowStart))) {
                    String key = dto.getId() + "_" + occStart;
                    if (addedKeys.add(key)) {
                        result.add(dto.withTimes(occStart, occEnd));
                        occCount++;
                    }
                }
                // 다음달 같은 N번째 X요일 계산
                occStart = nextNthWeekday(occStart, interval);
            }
            return;
        }

        // 기본 처리: DAILY, WEEKLY(단순), MONTHLY, YEARLY
        LocalDateTime occStart = dto.getStartAt();
        int occCount = 0;
        int safety = 0;
        while (!occStart.isAfter(windowEnd) && !occStart.isAfter(until) && safety++ < 3000 && occCount < maxCount) {
            LocalDateTime occEnd = duration != null ? occStart.plus(duration) : null;
            boolean overlaps = !occStart.isAfter(windowEnd)
                    && (occEnd == null || !occEnd.isBefore(windowStart))
                    && !occStart.isBefore(windowStart.minusDays(1));
            if (overlaps || !occStart.isBefore(windowStart)) {
                String key = dto.getId() + "_" + occStart;
                if (addedKeys.add(key)) {
                    result.add(dto.withTimes(occStart, occEnd));
                    occCount++;
                }
            }
            occStart = advance(occStart, type, interval);
        }
    }

    private DayOfWeek dayNumToDayOfWeek(int dayNum) {
        return switch (dayNum) {
            case 0 -> DayOfWeek.SUNDAY;
            case 1 -> DayOfWeek.MONDAY;
            case 2 -> DayOfWeek.TUESDAY;
            case 3 -> DayOfWeek.WEDNESDAY;
            case 4 -> DayOfWeek.THURSDAY;
            case 5 -> DayOfWeek.FRIDAY;
            default -> DayOfWeek.SATURDAY;
        };
    }

    private LocalDateTime nextNthWeekday(LocalDateTime current, int monthInterval) {
        // 현재가 몇 번째 주 무슨 요일인지 파악
        int dayOfMonth = current.getDayOfMonth();
        int nth = (dayOfMonth - 1) / 7 + 1; // 1~5
        DayOfWeek dow = current.getDayOfWeek();

        LocalDateTime base = current.plusMonths(monthInterval).withDayOfMonth(1);
        LocalDateTime first = base.with(TemporalAdjusters.firstInMonth(dow));
        LocalDateTime result = first.plusWeeks(nth - 1);
        // 같은 달인지 확인 (5번째 요일이 없는 경우 마지막으로)
        if (result.getMonth() != base.getMonth()) {
            result = first.plusWeeks(nth - 2);
        }
        return result.withHour(current.getHour()).withMinute(current.getMinute()).withSecond(current.getSecond());
    }

    private LocalDateTime advance(LocalDateTime dt, String type, int interval) {
        return switch (type) {
            case "DAILY"   -> dt.plusDays(interval);
            case "WEEKLY"  -> dt.plusWeeks(interval);
            case "MONTHLY" -> dt.plusMonths(interval);
            case "YEARLY"  -> dt.plusYears(interval);
            default        -> dt.plusYears(1000);
        };
    }

    private JsonNode parseRule(String recurrenceRule) {
        if (recurrenceRule == null || recurrenceRule.isBlank()) return null;
        try {
            return new ObjectMapper().readTree(recurrenceRule);
        } catch (Exception ignored) { return null; }
    }

    private LocalDateTime parseUntil(JsonNode rule, LocalDateTime fallback) {
        if (rule == null) return fallback;
        try {
            JsonNode until = rule.get("until");
            if (until != null && !until.isNull() && !until.asText().isBlank()) {
                return LocalDate.parse(until.asText()).atTime(23, 59, 59);
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    private int parseCount(JsonNode rule) {
        if (rule == null) return Integer.MAX_VALUE;
        JsonNode count = rule.get("count");
        if (count != null && !count.isNull() && count.asInt(0) > 0) return count.asInt();
        return Integer.MAX_VALUE;
    }

    private int parseInterval(JsonNode rule) {
        if (rule == null) return 1;
        JsonNode interval = rule.get("interval");
        int v = interval != null ? interval.asInt(1) : 1;
        return v < 1 ? 1 : v;
    }

    private LocalDateTime parseUntil(String recurrenceRule, LocalDateTime fallback) {
        return parseUntil(parseRule(recurrenceRule), fallback);
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
                .calendarType(request.getCalendarType() != null ? request.getCalendarType() : "GENERAL")
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
        calendar.update(request.getName(), request.getColor(), request.getDescription(), request.getCalendarType());
        if (request.getIsActive() != null) {
            calendar.updateActive(request.getIsActive());
        }
        // 삭제와 재삽입을 같은 트랜잭션에서 하면 Hibernate가 삽입을 삭제보다 먼저
        // 플러시해서 (calendar_id, group_id) 유니크 제약에 걸리므로 즉시 flush
        permissionRepository.deleteByCalendarId(id);
        permissionRepository.flush();
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
        Set<Long> seen = new HashSet<>();
        perms.stream()
                .filter(p -> p.getGroupId() != null && seen.add(p.getGroupId()))
                .forEach(p -> permissionRepository.save(CalendarGroupPermission.builder()
                        .calendarId(calendarId)
                        .groupId(p.getGroupId())
                        .canRead(p.getCanRead() != null ? p.getCanRead() : true)
                        .canWrite(p.getCanWrite() != null ? p.getCanWrite() : false)
                        .build()));
    }
}
