package com.corum.backend.domain.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CalendarGroupPermissionRepository extends JpaRepository<CalendarGroupPermission, Long> {

    List<CalendarGroupPermission> findByCalendarId(Long calendarId);

    void deleteByCalendarId(Long calendarId);

    @Query("""
        SELECT p FROM CalendarGroupPermission p
        WHERE p.calendarId = :calendarId AND p.groupId IN :groupIds
        """)
    List<CalendarGroupPermission> findByCalendarIdAndGroupIds(Long calendarId, List<Long> groupIds);
}
