package com.corum.backend.domain.calendar;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalendarRepository extends JpaRepository<CalendarEntity, Long> {

    List<CalendarEntity> findByIsActiveTrueOrderBySortOrderAscIdAsc();

    List<CalendarEntity> findAllByOrderBySortOrderAscIdAsc();
}
