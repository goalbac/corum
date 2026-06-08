package com.corum.backend.domain.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CalendarGroupPermissionRepository extends JpaRepository<CalendarGroupPermission, Long> {

    List<CalendarGroupPermission> findByCalendarId(Long calendarId);

    Optional<CalendarGroupPermission> findByCalendarIdAndGroupId(Long calendarId, Long groupId);

    void deleteByCalendarId(Long calendarId);

    // 회원의 그룹 목록으로 특정 캘린더 읽기 가능 여부
    @Query("""
        SELECT COUNT(p) > 0 FROM CalendarGroupPermission p
        WHERE p.calendarId = :calendarId AND p.groupId IN :groupIds AND p.canRead = true
        """)
    boolean canRead(Long calendarId, List<Long> groupIds);

    // 회원의 그룹 목록으로 특정 캘린더 쓰기 가능 여부
    @Query("""
        SELECT COUNT(p) > 0 FROM CalendarGroupPermission p
        WHERE p.calendarId = :calendarId AND p.groupId IN :groupIds AND p.canWrite = true
        """)
    boolean canWrite(Long calendarId, List<Long> groupIds);

    // 읽기 가능한 캘린더 ID 목록
    @Query("""
        SELECT DISTINCT p.calendarId FROM CalendarGroupPermission p
        WHERE p.groupId IN :groupIds AND p.canRead = true
        """)
    List<Long> findReadableCalendarIds(List<Long> groupIds);
}
