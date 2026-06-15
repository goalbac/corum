package com.corum.backend.domain.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    // 기간 내 일정 조회
    @Query("""
        SELECT e FROM CalendarEvent e
        WHERE e.calendarId IN :calendarIds
        AND e.startAt <= :end AND (e.endAt IS NULL OR e.endAt >= :start)
        ORDER BY e.startAt ASC
        """)
    List<CalendarEvent> findByPeriod(List<Long> calendarIds,
                                      LocalDateTime start, LocalDateTime end);

    // 반복 일정 중 종료일 이전에 시작하는 것들 (기간 내 occurrence 확장용)
    @Query("""
        SELECT e FROM CalendarEvent e
        WHERE e.calendarId IN :calendarIds
        AND e.recurrenceType != 'NONE'
        AND e.startAt <= :end
        """)
    List<CalendarEvent> findRecurringBeforeEnd(List<Long> calendarIds, LocalDateTime end);

    List<CalendarEvent> findByCalendarIdOrderByStartAtAsc(Long calendarId);

    void deleteByCalendarId(Long calendarId);
}
