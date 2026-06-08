package com.corum.backend.domain.dashboard;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DashboardWidgetRepository extends JpaRepository<DashboardWidget, Long> {
    List<DashboardWidget> findAllByOrderBySortOrderAscIdAsc();
    List<DashboardWidget> findByIsActiveTrueOrderBySortOrderAscIdAsc();
}
