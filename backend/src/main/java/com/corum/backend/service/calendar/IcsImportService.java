package com.corum.backend.service.calendar;

import com.corum.backend.domain.calendar.CalendarEvent;
import com.corum.backend.domain.calendar.CalendarEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IcsImportService {

    private final CalendarEventRepository calendarEventRepository;

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter DATETIME_UTC  = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    /** URL에서 ICS를 내려받아 파싱 후 저장 */
    @Transactional
    public Map<String, Integer> importFromUrl(Long calendarId, String url, int yearFrom, int yearTo, Long memberId)
            throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Corum/1.0")
                .GET().build();
        HttpResponse<InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream is = res.body()) {
            return importFromStream(calendarId, is, yearFrom, yearTo, memberId);
        }
    }

    /** InputStream에서 ICS를 파싱 후 저장 */
    @Transactional
    public Map<String, Integer> importFromStream(Long calendarId, InputStream is, int yearFrom, int yearTo, Long memberId)
            throws IOException {
        String icsText = unfold(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)));
        List<Map<String, String>> events = parseVEvents(icsText);

        // 기존 이벤트 키(제목|날짜)로 중복 방지
        Set<String> seen = new HashSet<>();
        calendarEventRepository.findByCalendarIdOrderByStartAtAsc(calendarId).forEach(e ->
                seen.add(e.getTitle() + "|" + e.getStartAt().toLocalDate()));

        int imported = 0, skipped = 0;
        List<CalendarEvent> toSave = new ArrayList<>();
        for (Map<String, String> ev : events) {
            List<CalendarEvent> built = buildEvents(calendarId, ev, yearFrom, yearTo, memberId);
            if (built.isEmpty()) { skipped++; continue; }
            for (CalendarEvent e : built) {
                String key = e.getTitle() + "|" + e.getStartAt().toLocalDate();
                if (seen.add(key)) {
                    toSave.add(e);
                } else {
                    skipped++;
                }
            }
        }
        calendarEventRepository.saveAll(toSave);
        imported = toSave.size();
        return Map.of("imported", imported, "skipped", skipped);
    }

    /** ICS line-unfolding: CRLF+공백을 이어붙임 */
    private String unfold(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.isEmpty() && (line.charAt(0) == ' ' || line.charAt(0) == '\t')) {
                sb.append(line.substring(1));
            } else {
                sb.append('\n').append(line);
            }
        }
        return sb.toString();
    }

    /** VCALENDAR 안의 VEVENT 블록을 파싱해서 key→value 맵 목록으로 반환 */
    private List<Map<String, String>> parseVEvents(String ics) {
        List<Map<String, String>> result = new ArrayList<>();
        String[] lines = ics.split("\n");
        Map<String, String> current = null;
        for (String raw : lines) {
            String line = raw.strip();
            if (line.equalsIgnoreCase("BEGIN:VEVENT")) {
                current = new LinkedHashMap<>();
            } else if (line.equalsIgnoreCase("END:VEVENT")) {
                if (current != null) { result.add(current); current = null; }
            } else if (current != null && line.contains(":")) {
                int colon = line.indexOf(':');
                String key = line.substring(0, colon).toUpperCase();
                String val = line.substring(colon + 1);
                // 파라미터가 있는 키 처리: DTSTART;VALUE=DATE → DTSTART
                String baseKey = key.contains(";") ? key.substring(0, key.indexOf(';')) : key;
                // 파라미터 포함 원래 키도 저장 (날짜 형식 판별용)
                current.put(baseKey, val);
                current.put(key, val); // 파라미터 포함 버전도 보관
            }
        }
        return result;
    }

    /** VEVENT 맵 → CalendarEvent 목록 (RRULE:FREQ=YEARLY 처리 포함) */
    private List<CalendarEvent> buildEvents(Long calendarId, Map<String, String> ev,
                                             int yearFrom, int yearTo, Long memberId) {
        String summary = unescape(ev.getOrDefault("SUMMARY", "")).trim();
        if (summary.isEmpty()) return List.of();

        String description = unescape(ev.getOrDefault("DESCRIPTION", ""));
        String location    = unescape(ev.getOrDefault("LOCATION", ""));
        String rrule       = ev.getOrDefault("RRULE", "");

        LocalDateTime startAt = parseDateTime(ev, "DTSTART");
        LocalDateTime endAt   = parseEndDateTime(ev, startAt);
        if (startAt == null) return List.of();

        boolean isAllDay = isAllDayEvent(ev, "DTSTART");
        boolean isYearly = rrule.toUpperCase().contains("FREQ=YEARLY");

        if (isYearly) {
            // YEARLY 반복 → yearFrom~yearTo 범위의 개별 이벤트로 전개
            List<CalendarEvent> list = new ArrayList<>();
            for (int y = yearFrom; y <= yearTo; y++) {
                LocalDateTime s = startAt.withYear(y);
                LocalDateTime e = endAt != null ? endAt.withYear(y) : null;
                list.add(buildEntity(calendarId, summary, description, location, s, e, isAllDay, memberId));
            }
            return list;
        } else {
            // 단일 이벤트: 연도 범위에 속하는 경우만 포함
            int year = startAt.getYear();
            if (year < yearFrom || year > yearTo) return List.of();
            return List.of(buildEntity(calendarId, summary, description, location, startAt, endAt, isAllDay, memberId));
        }
    }

    private CalendarEvent buildEntity(Long calendarId, String title, String description,
                                       String location, LocalDateTime startAt, LocalDateTime endAt,
                                       boolean isAllDay, Long memberId) {
        return CalendarEvent.builder()
                .calendarId(calendarId)
                .title(title)
                .description(description.isEmpty() ? null : description)
                .location(location.isEmpty() ? null : location)
                .startAt(startAt)
                .endAt(endAt != null ? endAt : startAt.toLocalDate().atTime(23, 59, 59))
                .isAllDay(isAllDay)
                .recurrenceType("NONE")
                .createdBy(memberId)
                .build();
    }

    private LocalDateTime parseDateTime(Map<String, String> ev, String baseKey) {
        // 파라미터 없는 버전 먼저 시도
        String val = ev.get(baseKey);
        if (val == null) return null;
        return toLocalDateTime(val);
    }

    private LocalDateTime parseEndDateTime(Map<String, String> ev, LocalDateTime startAt) {
        String val = ev.get("DTEND");
        if (val == null) return startAt != null ? startAt.toLocalDate().atTime(23, 59, 59) : null;
        LocalDateTime end = toLocalDateTime(val);
        if (end == null) return null;
        // ICS의 DTEND(VALUE=DATE)는 exclusive → 하루 빼서 inclusive로
        if (isDateOnly(val)) {
            end = end.minusDays(1).withHour(23).withMinute(59).withSecond(59);
        }
        return end;
    }

    private LocalDateTime toLocalDateTime(String val) {
        val = val.trim();
        try {
            if (val.length() == 8) {
                // VALUE=DATE: YYYYMMDD
                return LocalDate.parse(val, DATE_FMT).atStartOfDay();
            } else if (val.endsWith("Z")) {
                // UTC datetime: YYYYMMDDTHHmmssZ → KST(+9)
                LocalDateTime utc = LocalDateTime.parse(val, DATETIME_UTC);
                return utc.plusHours(9);
            } else if (val.contains("T")) {
                return LocalDateTime.parse(val.substring(0, 15), DATETIME_FMT);
            }
        } catch (Exception e) {
            log.warn("ICS datetime parse failed: {}", val);
        }
        return null;
    }

    private boolean isAllDayEvent(Map<String, String> ev, String baseKey) {
        // VALUE=DATE 파라미터가 있으면 종일
        for (String key : ev.keySet()) {
            if (key.startsWith(baseKey + ";") && key.contains("VALUE=DATE")) return true;
        }
        String val = ev.get(baseKey);
        return val != null && isDateOnly(val.trim());
    }

    private boolean isDateOnly(String val) {
        return val != null && val.trim().length() == 8 && !val.contains("T");
    }

    private String unescape(String s) {
        return s.replace("\\n", "\n").replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\");
    }
}
