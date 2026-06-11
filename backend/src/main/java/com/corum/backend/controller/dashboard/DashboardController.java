package com.corum.backend.controller.dashboard;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.dashboard.DashboardStatsResponse;
import com.corum.backend.dto.dashboard.DashboardWidgetResponse;
import com.corum.backend.service.dashboard.DashboardWidgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardWidgetService dashboardWidgetService;

    /** 전체 위젯 데이터 (기존 호환용 — 사용 지양) */
    @GetMapping("/widgets")
    public ApiResponse<List<DashboardWidgetResponse>> getWidgets() {
        return ApiResponse.ok(dashboardWidgetService.getActiveWidgets());
    }

    /** 위젯 레이아웃만 (메타데이터, 데이터 없음 — 빠름) */
    @GetMapping("/widgets/layout")
    public ApiResponse<List<DashboardWidgetResponse>> getWidgetLayouts() {
        return ApiResponse.ok(dashboardWidgetService.getActiveWidgetLayouts());
    }

    /** 단일 위젯 데이터 로드 */
    @GetMapping("/widgets/{id}")
    public ApiResponse<DashboardWidgetResponse> getWidgetData(@PathVariable Long id) {
        return ApiResponse.ok(dashboardWidgetService.getWidgetData(id));
    }

    @GetMapping("/stats")
    public ApiResponse<DashboardStatsResponse> getStats() {
        return ApiResponse.ok(dashboardWidgetService.getStats());
    }
}
