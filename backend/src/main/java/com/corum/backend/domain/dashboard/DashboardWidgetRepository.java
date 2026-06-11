package com.corum.backend.domain.dashboard;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DashboardWidgetRepository extends JpaRepository<DashboardWidget, Long> {

    List<DashboardWidget> findAllByOrderBySortOrderAscIdAsc();

    List<DashboardWidget> findByIsActiveTrueOrderBySortOrderAscIdAsc();

    /** 특정 대시보드(menuId)의 전체 위젯 (menuId=null → 홈 대시보드) */
    @Query("SELECT w FROM DashboardWidget w WHERE (:menuId IS NULL AND w.menuId IS NULL) OR w.menuId = :menuId ORDER BY w.sortOrder ASC, w.id ASC")
    List<DashboardWidget> findByMenuId(@Param("menuId") Long menuId);

    /** 특정 대시보드의 활성 위젯 */
    @Query("SELECT w FROM DashboardWidget w WHERE w.isActive = true AND ((:menuId IS NULL AND w.menuId IS NULL) OR w.menuId = :menuId) ORDER BY w.sortOrder ASC, w.id ASC")
    List<DashboardWidget> findActiveByMenuId(@Param("menuId") Long menuId);

    /** 특정 대시보드의 최대 sortOrder */
    @Query("SELECT COALESCE(MAX(w.sortOrder), -1) FROM DashboardWidget w WHERE (:menuId IS NULL AND w.menuId IS NULL) OR w.menuId = :menuId")
    int findMaxSortOrderByMenuId(@Param("menuId") Long menuId);

    @Query("SELECT COALESCE(MAX(w.sortOrder), -1) FROM DashboardWidget w")
    int findMaxSortOrder();
}
