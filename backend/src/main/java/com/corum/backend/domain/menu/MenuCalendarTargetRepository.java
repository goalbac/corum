package com.corum.backend.domain.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MenuCalendarTargetRepository extends JpaRepository<MenuCalendarTarget, Long> {

    List<MenuCalendarTarget> findByMenuId(Long menuId);

    List<MenuCalendarTarget> findByMenuIdIn(List<Long> menuIds);

    @Query("SELECT t.calendarId FROM MenuCalendarTarget t WHERE t.menuId = :menuId")
    List<Long> findCalendarIdsByMenuId(Long menuId);

    @Modifying
    @Query("DELETE FROM MenuCalendarTarget t WHERE t.menuId = :menuId")
    void deleteByMenuId(Long menuId);
}
