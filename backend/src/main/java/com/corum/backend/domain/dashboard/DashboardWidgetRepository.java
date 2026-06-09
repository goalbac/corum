package com.corum.backend.domain.dashboard;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DashboardWidgetRepository extends JpaRepository<DashboardWidget, Long> {
    List<DashboardWidget> findAllByOrderBySortOrderAscIdAsc();
    List<DashboardWidget> findByIsActiveTrueOrderBySortOrderAscIdAsc();

    @Query("SELECT COALESCE(MAX(w.sortOrder), -1) FROM DashboardWidget w")
    int findMaxSortOrder();
}
